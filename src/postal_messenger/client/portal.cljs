(ns postal-messenger.client.portal
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [postal-messenger.client.util.misc :as misc]
            [postal.client :as pc]
            [beicon.core :as s]))

(def client (pc/client (str (misc/url-origin) "/postal")))

(def subscribe-on-mount
  {:did-mount (fn [state]
                (let [message-bus (-> state :rum/args first)
                      [instream outbus] (pc/socket client :messages {:token (misc/get-jwt)})]
                  (s/on-value instream (fn [message]
                                         (println "MESSAGE" message)))
                  (s/on-end instream #(.info js/console "Message websocket closed"))
                  (do! message-bus #(assoc % :messages-bus outbus))
                  state))})

(rum/defc root < subscribe-on-mount
          [message-bus state]
          (let [messages (:messages-bus state)]
            [:div
             [:input {:type      "text"
                      :on-change (fn [e] (do! message-bus #(assoc % :input-value (aget e "target" "value"))))}]
             [:button {:on-click #(s/push! messages {:data (:input-value state)})} "Send"]]))