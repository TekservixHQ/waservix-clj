(ns tekservix.waservix
  "WAservix WhatsApp API Gateway: Clojure client by Tekservix.

  Send WhatsApp messages from your own number. A thin wrapper over the official
  Java client (com.tekservix/waservix on Maven Central); every call returns plain
  Clojure data (maps with keyword keys, vectors).

  You need a Tekservix account with WAservix and your number's device API key
  (it starts with wpk_): https://tekservix.com/waservix"
  (:require [clojure.walk :as walk])
  (:import (com.tekservix.waservix WAservix WAservixException)
           (com.google.gson JsonElement JsonObject JsonPrimitive)))

(set! *warn-on-reflection* true)

(defn json->clj
  "Turns a Gson JsonElement into Clojure data."
  [^JsonElement e]
  (cond
    (or (nil? e) (.isJsonNull e)) nil
    (.isJsonObject e) (into {} (map (fn [[k v]] [(keyword k) (json->clj v)]))
                            (.entrySet ^JsonObject (.getAsJsonObject e)))
    (.isJsonArray e) (mapv json->clj (.getAsJsonArray e))
    :else (let [^JsonPrimitive p (.getAsJsonPrimitive e)]
            (cond (.isBoolean p) (.getAsBoolean p)
                  (.isNumber p) (let [d (.doubleValue (.getAsNumber p))]
                                  (if (and (== d (Math/rint d)) (< (Math/abs d) 9.0E15)) (long d) d))
                  :else (.getAsString p)))))

(defn- ->java
  "Clojure data -> what Gson serialises cleanly: string keys all the way down, keywords as names."
  [x]
  (walk/postwalk (fn [v] (cond (map? v) (into {} (map (fn [[k vv]] [(if (keyword? k) (name k) (str k)) vv])) v)
                               (keyword? v) (name v)
                               :else v))
                 x))

(defn client
  "Creates a client. Options: :base-url, :timeout-ms (default 30000).

  (def wa (client (System/getenv \"WASERVIX_API_KEY\")))"
  (^WAservix [api-key] (WAservix. ^String api-key))
  (^WAservix [api-key {:keys [base-url timeout-ms] :or {base-url WAservix/DEFAULT_BASE_URL timeout-ms 30000}}]
   (WAservix. ^String api-key ^String base-url (int timeout-ms))))

(defmacro ^:private call [form]
  `(try (json->clj ~form)
        (catch WAservixException e#
          (throw (ex-info (.getMessage e#) {:status (.getStatus e#)} e#)))))

(defn send-message
  "Sends any message type to a number or a group ID (…@g.us). Field names follow the API:
  :text :imageUrl :videoUrl :audioUrl :documentUrl :fileName :location :poll :replyTo :mentions …

  (send-message wa \"+250788123456\" {:location {:latitude -1.9441 :longitude 30.0619 :name \"Our shop\"}})"
  [^WAservix c to fields]
  (call (.sendMessage c ^String to ^java.util.Map (->java fields))))

(defn send-text "(send-text wa \"+250788123456\" \"Your order has shipped\")"
  [^WAservix c to text] (call (.sendText c ^String to ^String text)))
(defn send-image "Image from a public URL, with an optional caption."
  [^WAservix c to image-url & [caption]] (call (.sendImage c ^String to ^String image-url ^String caption)))
(defn send-document "Document from a public URL, with an optional file name and caption."
  [^WAservix c to document-url & [file-name caption]]
  (call (.sendDocument c ^String to ^String document-url ^String file-name ^String caption)))
(defn message-info "Status of a sent message: queued, sent, delivered, read, played or failed."
  [^WAservix c msg-id] (call (.getMessageInfo c (long msg-id))))
(defn edit-message "Edits a sent text (WhatsApp allows about 15 minutes)."
  [^WAservix c msg-id text] (call (.editMessage c (long msg-id) ^String text)))
(defn delete-message "Deletes a message for everyone." [^WAservix c msg-id] (call (.deleteMessage c (long msg-id))))
(defn resend-message "Sends a failed message again." [^WAservix c msg-id] (call (.resendMessage c (long msg-id))))
(defn on-whatsapp? "Does this number have WhatsApp? Returns the API answer, e.g. {:exists true …}."
  [^WAservix c phone] (call (.onWhatsApp c ^String phone)))
(defn status "Connection status of your number." [^WAservix c] (call (.getStatus c)))
(defn list-contacts "Contacts of your number." [^WAservix c] (call (.listContacts c)))
(defn get-contact "One contact." [^WAservix c phone] (call (.getContact c ^String phone)))
(defn save-contact "Saves a contact name." [^WAservix c phone contact-name] (call (.saveContact c ^String phone ^String contact-name)))
(defn list-groups "Groups your number is in." [^WAservix c] (call (.listGroups c)))
(defn group-metadata "One group's details; group-id ends in @g.us." [^WAservix c group-id] (call (.getGroupMetadata c ^String group-id)))
(defn request "Any other API call: (request wa :get \"/user\")."
  [^WAservix c method path & [body]]
  (call (.request c (.toUpperCase (name method)) ^String path (some-> body ->java))))

(defn verify-webhook
  "Checks the X-Webhook-Signature header of a webhook call against your number's webhook secret."
  [signature secret]
  (WAservix/verifyWebhook ^String signature ^String secret))
