(ns tekservix.waservix-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [tekservix.waservix :as wa])
  (:import (com.sun.net.httpserver HttpServer HttpHandler HttpExchange)
           (java.net InetSocketAddress)))

(def ^:private last-call (atom nil))
(def ^:private reply (atom {:status 200 :body "{}"}))
(def ^:private base (atom nil))

(defn- start! []
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext server "/"
                    (reify HttpHandler
                      (handle [_ ex]
                        (let [^HttpExchange ex ex
                              body (slurp (.getRequestBody ex))
                              {:keys [status] ^String out :body} @reply
                              bytes (.getBytes out "UTF-8")]
                          (reset! last-call {:method (.getRequestMethod ex)
                                             :path (.getRawPath (.getRequestURI ex))
                                             :auth (.getFirst (.getRequestHeaders ex) "Authorization")
                                             :body body})
                          (.add (.getResponseHeaders ex) "Content-Type" "application/json")
                          (.sendResponseHeaders ex (int status) (alength bytes))
                          (doto (.getResponseBody ex) (.write bytes) (.close))))))
    (.start server)
    (reset! base (str "http://127.0.0.1:" (.getPort (.getAddress server)) "/api"))
    server))

(use-fixtures :once (fn [t] (let [s (start!)] (try (t) (finally (.stop ^HttpServer s 0))))))

(defn- c [] (wa/client "wpk_test" {:base-url @base}))

(deftest send-text-test
  (reset! reply {:status 200 :body "{\"data\":{\"msgId\":5473,\"status\":\"in_progress\"}}"})
  (is (= 5473 (:msgId (wa/send-text (c) "+250788123456" "Hello"))))
  (is (= "POST" (:method @last-call)))
  (is (= "/api/send-message" (:path @last-call)))
  (is (= "Bearer wpk_test" (:auth @last-call)))
  (is (re-find #"\"text\":\"Hello\"" (:body @last-call))))

(deftest nested-fields-use-string-keys
  (reset! reply {:status 200 :body "{\"data\":{\"msgId\":1}}"})
  (wa/send-message (c) "+250788123456" {:location {:latitude -1.9441 :longitude 30.0619 :name "Our shop"}})
  (is (re-find #"\"location\":\{" (:body @last-call)))
  (is (re-find #"\"latitude\":-1.9441" (:body @last-call)))
  (is (not (re-find #":latitude" (:body @last-call)))))

(deftest group-id-is-sent-unencoded
  (reset! reply {:status 200 :body "{\"data\":{\"subject\":\"Tekservix\"}}"})
  (is (= "Tekservix" (:subject (wa/group-metadata (c) "120363000000000000@g.us"))))
  (is (= "/api/groups/120363000000000000@g.us/metadata" (:path @last-call))))

(deftest errors-become-ex-info
  (reset! reply {:status 401 :body "{\"message\":\"Use the device API key\"}"})
  (let [e (try (wa/status (c)) nil (catch clojure.lang.ExceptionInfo e e))]
    (is (some? e))
    (is (= 401 (:status (ex-data e))))))

(deftest webhook-secret
  (is (true? (wa/verify-webhook "s3cret" "s3cret")))
  (is (false? (wa/verify-webhook "wrong" "s3cret")))
  (is (false? (wa/verify-webhook nil "s3cret"))))
