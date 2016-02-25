(ns postal-messenger.client.util.messaging
  (:require [postal-messenger.client.util.http :as http]
            [cljs-uuid-utils.core :as uuid]))

(defn send-event!
  ([socket_id type] (send-event! socket_id type nil))
  ([socket_id type data]
   (http/post! "/api/message" (merge
                                {                           ;:id        (uuid/uuid-string (uuid/make-random-uuid))
                                 :dest      :phone
                                 :type      type
                                 :socket_id socket_id}
                                (when data {:data data})))))

(defn send-message!
  [socket_id idx conv msg]
  (send-event! socket_id :send-message {:type       :sent
                                        :idx        idx
                                        :recipients (:recipients conv)
                                        :text       msg}))

(defn get-contacts!
  [socket_id]
  (send-event! socket_id :get-contacts))

(defn get-conversations!
  [socket_id]
  (send-event! socket_id :get-conversations))

(defn get-conversation!
  [socket_id thread_id]
  (send-event! socket_id :get-conversation {:thread_id thread_id}))