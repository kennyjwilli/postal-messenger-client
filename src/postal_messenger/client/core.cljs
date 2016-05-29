(ns postal-messenger.client.core
  (:require [griebenschmalz.core :as g]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
            [postal-messenger.client.views.core :as view]
            [postal-messenger.client.util.misc :as misc]
            [postal-messenger.client.schema :as schema]
            [datascript.core :as d]))

(enable-console-print!)

(def initial-state
  (let [db (d/empty-db schema/schema)]
    {:socket_id             ""
     :db                    db
     :selected-conversation nil
     :conversations         nil}))

(defn render-fn
  [message-bus state]
  (rum/mount (view/root message-bus state) (.querySelector js/document "#app")))

(defn ^:export main
  []
  (let [jwt (misc/get-jwt)]
    (if (nil? jwt)
      (let [current-url (url (misc/get-url))
            login-url (assoc current-url :path "/login.html"
                                         :query {:redirect current-url})]
        (misc/http-redirect login-url))
      (g/start initial-state render-fn))))