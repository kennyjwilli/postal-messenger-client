(ns postal-messenger.client.core
  (:require [griebenschmalz.core :as g]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
    #_[postal-messenger.client.portal :refer [root]]
            [postal-messenger.client.views.core :as view]
            [postal-messenger.client.util.misc :as misc]
            [postal-messenger.client.util.messaging :as msg]
            [cljs-time.core :as t]))

(enable-console-print!)

(def initial-state
  {:input-value           ""
   :socket_id             ""
   :selected-conversation nil
   :conversations         {(msg/conversation-id [{:phone      "4445556666"
                                                  :first-name "Mary"
                                                  :last-name  "Smith"}])
                           {:recipients  [{:phone      "4445556666"
                                           :first-name "Mary"
                                           :last-name  "Smith"}]
                            :last-update (t/date-time 2016 2 10 8 13)
                            :messages    [{:data      "mary message"
                                           :type      "sent"
                                           :timestamp (t/date-time 2016 2 10 8 12)}
                                          {:data      "recieved mary message"
                                           :type      "received"
                                           :timestamp (t/date-time 2016 2 10 8 13)}]}
                           (msg/conversation-id [{:phone      "1112223333"
                                                  :first-name "John"
                                                  :last-name  "Example"}])
                           {:recipients  [{:phone      "1112223333"
                                           :first-name "John"
                                           :last-name  "Example"}]
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