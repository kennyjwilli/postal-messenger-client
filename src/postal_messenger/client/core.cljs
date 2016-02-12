(ns postal-messenger.client.core
  (:require [griebenschmalz.core :as g]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
            #_[postal-messenger.client.portal :refer [root]]
            [postal-messenger.client.views.core :as view]
            [postal-messenger.client.util.misc :as misc]))

(def initial-state
  {:input-value ""
   :socket_id   ""
   :messages    [{:data "first"
                  :type "sent"}
                 {:data "second"
                  :type "received"}]})

(defn render-fn
  [message-bus state]
  (rum/mount (view/root message-bus state) (.querySelector js/document "#app")))

(defn ^:export main
  []
  (enable-console-print!)
  (let [jwt "" #_(misc/get-jwt)]
    (if (nil? jwt)
      (let [current-url (url (misc/get-url))
            login-url (assoc current-url :path "/login.html"
                                         :query {:redirect current-url})]
        (misc/http-redirect login-url))
      (g/start initial-state render-fn))))