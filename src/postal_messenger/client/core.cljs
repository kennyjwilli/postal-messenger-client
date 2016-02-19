(ns postal-messenger.client.core
  (:require [griebenschmalz.core :as g]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
            [postal-messenger.client.views.core :as view]
            [postal-messenger.client.util.misc :as misc]
            [postal-messenger.client.util.messaging :as msg]
            [cljs-time.core :as t]))

(enable-console-print!)

(def mary
  {:phoneNumbers [{:number "4445556666"}]
   :name         "Mary Smith"})

(def john
  {:phoneNumbers [{:number "2223334444"}]
   :name         "John Example"})

(def initial-state
  {:socket_id             ""
   :selected-conversation nil
   :contacts              {"4445556666" mary
                           "1112223333" john}
   :conversations         {(msg/conversation-id [mary])
                           {:recipients  [mary]
                            :last-update (t/date-time 2016 2 18 8 13)
                            :messages    [{:data      "mary message"
                                           :type      "sent"
                                           :timestamp (t/date-time 2016 2 10 8 12)}
                                          {:data      "recieved mary message"
                                           :type      "received"
                                           :timestamp (t/date-time 2016 2 18 8 13)}]}
                           (msg/conversation-id [john])
                           {:recipients  [john]
                            :last-update (t/date-time 2016 2 12 10 55)
                            :messages    [{:data      "first"
                                           :type      "sent"
                                           :timestamp (t/date-time 2016 2 12 10 54)}
                                          {:data      "second"
                                           :type      "received"
                                           :timestamp (t/date-time 2016 2 12 10 55)}]}}})

(defn render-fn
  [message-bus state]
  (rum/mount (view/root message-bus state) (.querySelector js/document "#app")))

(defn ^:export main
  []
  (let [jwt "" #_(misc/get-jwt)]
    (if (nil? jwt)
      (let [current-url (url (misc/get-url))
            login-url (assoc current-url :path "/login.html"
                                         :query {:redirect current-url})]
        (misc/http-redirect login-url))
      (g/start initial-state render-fn))))