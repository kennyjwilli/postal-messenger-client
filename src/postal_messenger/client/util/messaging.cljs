(ns postal-messenger.client.util.messaging
  (:require [postal-messenger.client.util.http :as http]))

(defn send-message!
  [socket_id sender data]
  (http/post! "/api/message" {:dest      :phone
                              :type      :send-message
                              :socket_id socket_id
                              :message   {:type   :sent
                                          :sender sender
                                          :data   data}}))