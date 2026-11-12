# WAservix WhatsApp API Gateway: Clojure client

**WAservix WhatsApp API Gateway** - Clojure client by Tekservix. Send WhatsApp messages, images, documents and locations from your own number.

**You need a Tekservix account with WAservix and a WhatsApp number connected in the dashboard.** Each number has its own API key (it starts with `wpk_`) under WAservix → Devices. [Create an account](https://tekservix.com/waservix) · [API docs](https://tekservix.com/waservix/docs/)

A thin wrapper over the official Java client ([com.tekservix/waservix](https://central.sonatype.com/artifact/com.tekservix/waservix) on Maven Central). Every call returns plain Clojure data.

## Install

deps.edn:

```clojure
com.tekservix/waservix-clj {:mvn/version "1.0.0"}
```

Leiningen:

```clojure
[com.tekservix/waservix-clj "1.0.0"]
```

## Use

```clojure
(require '[tekservix.waservix :as wa])

(def c (wa/client (System/getenv "WASERVIX_API_KEY")))

(wa/send-text c "+250788123456" "Your order #1042 has shipped.")
;; => {:msgId 5473 :status "in_progress" ...}

(wa/send-image c "+250788123456" "https://example.com/catalogue.jpg" "New arrivals")
(wa/send-document c "+250788123456" "https://example.com/inv-1042.pdf" "Invoice-1042.pdf" "Your invoice")
(wa/send-message c "+250788123456" {:location {:latitude -1.9441 :longitude 30.0619 :name "Our shop"}})

;; Groups (IDs end in @g.us)
(wa/list-groups c)
(wa/send-text c "120363000000000000@g.us" "Weekly offers are live.")

(wa/message-info c 5473)          ; queued, sent, delivered, read, played or failed
(wa/edit-message c 5473 "Hello again")
(wa/delete-message c 5473)
(wa/on-whatsapp? c "+250788123456")
(wa/status c)

;; In your webhook handler
(wa/verify-webhook (get headers "x-webhook-signature") secret)

;; Anything else in the API
(wa/request c :get "/user")
```

Errors are thrown as `ex-info` with `{:status 401}` (or 404, 429 ...) in `ex-data`.

## More WAservix integrations

[Java/Kotlin](https://github.com/TekservixHQ/waservix-java) · [npm](https://www.npmjs.com/package/waservix) · [PyPI](https://pypi.org/project/waservix/) · [All docs](https://tekservix.com/waservix/docs/)

support@tekservix.com · MIT licensed.
