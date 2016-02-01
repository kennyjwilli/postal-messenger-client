(ns postal-messenger.client.core
  (:require [griebenschmalz.core :as g]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
            [postal-messenger.client.portal :refer [root]]
            [postal-messenger.client.util.misc :as misc]))

(def initial-state
  {:input-value ""
   :messages    [{:data "first"
                  :type :sent} {:data "second"
                                :type :received}]})

(defn render-fn
  [message-bus state]
  (rum/mount (root message-bus state) (.querySelector js/document "#app")))

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