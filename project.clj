(defproject com.tekservix/waservix-clj "1.0.0"
  :description "WAservix WhatsApp API Gateway: Clojure client by Tekservix. Send WhatsApp messages, images, documents and locations from your own number."
  :url "https://tekservix.com/waservix"
  :license {:name "MIT" :url "https://opensource.org/licenses/MIT"}
  :scm {:name "git" :url "https://github.com/TekservixHQ/waservix-clj"}
  :pom-addition [:developers [:developer [:name "Tekservix"] [:email "support@tekservix.com"] [:url "https://tekservix.com"]]]
  :dependencies [[org.clojure/clojure "1.11.4" :scope "provided"]
                 [com.tekservix/waservix "1.0.5"]]
  :global-vars {*warn-on-reflection* true}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])
