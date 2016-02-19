(ns postal-messenger.client.util.messaging
  (:require [postal-messenger.client.util.http :as http]
            [cljs-time.core :as t]
            [cljs-time.coerce :as ct]
            [cljs-time.format :as fmt]))

(defn conversation-id
  "Returns a unique id for a given set of recipients."
  [recipients]
  (hash (sort-by hash (mapv #(select-keys % [:name :phoneNumbers]) recipients))))

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

(defn normalize-message
  [msg]
  (update msg :timestamp (fn [timestamp]
                           (fmt/parse (:date-hour-minute-second-ms fmt/formatters) timestamp))))

(defn send-message!
  [socket_id idx conv msg]
  (http/post! "/api/message" {:dest      :phone
                              :type      :send-message
                              :socket_id socket_id
                              :message   {:type       :sent
                                          :idx        idx
                                          :recipients (:recipients conv)
                                          :data       msg}}))