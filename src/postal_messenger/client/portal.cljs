(ns postal-messenger.client.portal
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [postal-messenger.client.util.misc :as misc]
            [pusher.core :as pusher]
            [beicon.core :as s]
            [promesa.core :as p]
            [postal-messenger.client.util.http :as http]
            [postal-messenger.client.util.messaging :as msg]
            [postal-messenger.client.views.core :as core-view]))

(defn- message-handler
  [msg message-bus]
  (println (type msg) msg)
  (when (= (:dest msg) "client")
    (condp = (:type msg)
      "add-message" (do! message-bus (fn [s] (update s :messages #(conj % (:message msg))))))))

(defn- connect-pusher
  [channel api-key message-bus]
  (let [p (pusher/pusher api-key {:authEndpoint "/api/pusher-auth"
                                  :auth         {:headers (misc/jwt-headers)}})
        channel (pusher/channel p channel)
        pusher-bus (pusher/subscribe channel "messages" {:parse-fn http/decode-json})
        #_stream #_(-> pusher-bus (s/filter #(= (:dest %) :client)))]
    (pusher/on-connected p (fn [] (do! message-bus (fn [s] (assoc s :socket_id (pusher/socket-id p))))))
    (s/on-value pusher-bus #(message-handler % message-bus))))

(def subscribe-on-mount
  {:did-mount (fn [state]
                (let [message-bus (-> state :rum/args first)]
                  (p/then (http/get! "/api/pusher")
                          (fn [{body :body}]
                            (connect-pusher (:message-channel body) (:api-key body) message-bus)))
                  state))})

(rum/defc root < subscribe-on-mount
          [message-bus state]
          (let [messages (:messages state)]

            #_[:div {:style {:width "300px"}}
             [:div.layout.vertical
              (map (fn [msg]
                     [:div {:class (str (:type msg) " message")}
                      [:span (:data msg)]]) messages)]
             [:div.layout.horizontal
              [:input.flex {:type      "text"
                            :on-change (fn [e] (do! message-bus #(assoc % :input-value (aget e "target" "value"))))}]
              [:button {:on-click (fn []
                                    (let [id (:socket_id state)]
                                      (when id
                                       (msg/send-message! id "1112223333" (:input-value state)))))} "Send"]]]))