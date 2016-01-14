(ns postal-messenger.client.login
  (:require [griebenschmalz.core :refer [do!] :as g]
            [httpurr.client.xhr :as http]
            [promesa.core :as p]
            [rum.core :as rum]
            [cemerick.url :refer [url]]
            [postal-messenger.client.util.misc :as misc]))

(def initial-state
  {:username      "john@example.com"
   :password      "Password1"
   :error-message ""})

(defn login-handler
  [message-bus state]
  (-> (p/then (http/post "/login" {:body    (select-keys state [:username :password])
                                   :headers {:content-type "application/edn"}})
              (fn [resp]
                (misc/set-jwt! (:body resp))
                (let [url (url (misc/get-url))
                      redirect-url (or (get-in url [:query "redirect"]) (misc/url-origin))]
                  (misc/http-redirect redirect-url))))
      (p/catch (fn [err] (do! message-bus #(assoc % :error-message "Incorrect login"))))))

(rum/defc root
          [message-bus state]
          [:div
           [:div
            [:label "Username"]
            [:br]
            [:input {:type      "text"
                     :value     "john@example.com"
                     :on-change (fn [e] (do! message-bus #(assoc % :username (aget e "target" "value"))))}]]
           [:div
            [:label "Password"]
            [:br]
            [:input {:type      "password"
                     :value     "Password1"
                     :on-change (fn [e] (do! message-bus #(assoc % :password (aget e "target" "value"))))}]]
           [:button {:on-click #(login-handler message-bus state)} "Login"]
           [:span (:error-message state)]])

(defn render-fn
  [message-bus state]
  (rum/mount (root message-bus state) (.querySelector js/document "#app")))

(defn ^:export main
  []
  (enable-console-print!)
  (g/start initial-state render-fn))