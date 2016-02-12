(ns postal-messenger.client.util.messaging
  (:require [postal-messenger.client.util.http :as http]
            [cljs-time.core :as t]))

(defn conversation-id
  "Returns a unique id for a given set of recipients."
  [recipients]
  (hash (sort-by hash recipients)))

(defn time-comparator
  "Sort time by closest time first"
  [x y]
  (cond
    (t/equal? x y) 0
    (t/after? x y) -1
    :default 1))

(defn sort-conversations
  "Sorts conversations by the newest conversation first"
  [convs]
  (into (sorted-map-by (fn [x y]
                         (time-comparator (get-in convs [x :last-update])
                                          (get-in convs [y :last-update])))) convs))

(defn send-message!
  [socket_id sender data]
  (http/post! "/api/message" {:dest      :phone
                              :type      :send-message
                              :socket_id socket_id
                              :message   {:type   :sent
                                          :sender sender
                                          :data   data}}))