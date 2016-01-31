(ns postal-messenger.client.portal
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [postal-messenger.client.util.misc :as misc]
            [postal.client :as pc]
            [pusher.core :as pusher]
            [beicon.core :as s]
            [promesa.core :as p]
            [postal-messenger.client.util.http :as http]))

(def client (pc/client (str (misc/url-origin) "/postal")))

(defn connect-pusher
  [channel api-key message-bus]
  (let [pusher (pusher/pusher api-key {:authEndpoint "/api/pusher-auth"
                                       :auth         {:headers (misc/jwt-headers)}})
        x (pusher/on-connected pusher (fn [] (println "CONNECTED")))
        x (pusher/on-error pusher (fn [] (println "ERROR")))
        channel (pusher/channel pusher channel)]))

(def subscribe-on-mount
  {:did-mount (fn [state]
                (let [message-bus (-> state :rum/args first)
                      [instream outbus] (pc/socket client :messages {:token (misc/get-jwt)})]
                  (s/on-value instream (fn [message]
                                         (println "MESSAGE" message)))
                  (s/on-end instream #(.info js/console "Message websocket closed"))
                  (p/then (http/get! "/api/pusher")
                          (fn [{body :body}]
                            (connect-pusher (:message-channel body) (:api-key body) message-bus)))
                  (do! message-bus #(assoc % :messages-bus outbus))
                  state))})

(rum/defc root < subscribe-on-mount
          [message-bus state]
          (let [messages (:messages-bus state)]
            [:div
             [:input {:type      "text"
                      :on-change (fn [e] (do! message-bus #(assoc % :input-value (aget e "target" "value"))))}]
             [:button {:on-click #(s/push! messages {:data (:input-value state)})} "Send"]]))