(ns postal-messenger.client.views.core
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [cljs-time.core :as t]
            [postal-messenger.client.util.messaging :as msg]))

(def conversations
  {(msg/conversation-id [{:phone      "4445556666"
                          :first-name "Mary"
                          :last-name  "Smith"}])
   {:recipients  [{:phone      "4445556666"
                   :first-name "Mary"
                   :last-name  "Smith"}]
    :last-update (t/date-time 2016 2 10 8 13)
    :messages    [{:data      "mary message"
                   :type      "sent"
                   :timestamp 1}
                  {:data      "recieved mary message"
                   :type      "received"
                   :timestamp 2}]}
   (msg/conversation-id [{:phone      "1112223333"
                          :first-name "John"
                          :last-name  "Example"}])
   {:recipients  [{:phone      "1112223333"
                   :first-name "John"
                   :last-name  "Example"}]
    :last-update (t/date-time 2016 2 11 10 55)
    :messages    [{:data      "first"
                   :type      "sent"
                   :timestamp 1}
                  {:data      "second"
                   :type      "received"
                   :timestamp 2}]}})

(rum/defc root
          [message-bus state]
          [:div])