(ns postal-messenger.client.core
  (:require [griebenschmalz.core :as g]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
            [postal-messenger.client.views.core :as view]
            [postal-messenger.client.util.misc :as misc]
            [postal-messenger.client.util.messaging :as msg]
            [postal-messenger.client.schema :as schema]
            [cljs-time.core :as t]
            [datascript.core :as d]))

(enable-console-print!)

(def mary-num "4445556666")
(def john-num "2223334444")

(def initial-state
  (let [mary-id (d/tempid :db.part/db)
        john-id (d/tempid :db.part/db)
        db (d/db-with (d/empty-db schema/schema)
                      [{:db/id           mary-id
                        :contact/name    "Mary Smith"
                        :contact/numbers [{:number/number mary-num :number/type "Mobile"}]}
                       {:db/id           john-id
                        :contact/name    "John Example"
                        :contact/numbers [{:number/number john-num :number/type "Mobile"}]}])]
    {:socket_id             ""
     :compose-pane-h        "50px"
     :db                    db
     :selected-conversation nil
     :conversations         {(misc/conversation-id [mary-num])
                             {:recipients  [mary-num]
                              :last-update (t/date-time 2016 2 18 8 13)
                              :messages    [{:text "mary message"
                                             :type "sent"
                                             :date (t/date-time 2016 2 10 8 12)}
                                            {:text "recieved mary message"
                                             :type "received"
                                             :date (t/date-time 2016 2 18 8 13)}]}
                             (misc/conversation-id [john-num])
                             {:recipients  [john-num]
                              :last-update (t/date-time 2016 2 12 10 55)
                              :messages    [{:text "first"
                                             :type "sent"
                                             :date (t/date-time 2016 2 12 10 54)}
                                            {:text "second"
                                             :type "received"
                                             :date (t/date-time 2016 2 12 10 55)}]}}}))

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