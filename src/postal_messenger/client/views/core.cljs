(ns postal-messenger.client.views.core
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [cats.labs.lens :as l]
            [cljs-time.core :as t]
            [postal-messenger.client.util.messaging :as m]
            [postal-messenger.client.util.misc :as misc]
            [clojure.string :as str]
            [postal-messenger.client.util.http :as http]
            [promesa.core :as p]
            [beicon.core :as s]
            [pusher.core :as pusher]))

;;====================================
;; HELPERS
;;====================================

(defn- message-handler
  [msg message-bus]
  (when (= (:dest msg) "client")
    (condp = (:type msg)
      "add-message" (do! message-bus (fn [s]
                                       ;; TODO: Normalize message (e.g. Change timestamp to be a cljs-time obj)
                                       (let [message (:message msg)
                                             id (m/conversation-id (:recipients message))
                                             s (update-in s [:conversations id :messages] #(conj % message))]
                                         (assoc-in s [:conversations id :last-update] (:timestamp message))))))))

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

(defn scroll-messages-to-bottom
  []
  (js/setTimeout #(misc/scroll-to-bottom (.querySelector js/document "#message-list-container"))))

;;====================================
;; UI
;;====================================

(rum/defc conversation-item
          [message-bus state id]
          (let [conv (get-in state [:conversations id])]
            [:div {:class    (str "conv noselect pointer " (when (= (:selected-conversation state) id) "selected"))
                   :key      (str "conv" id)
                   :on-click (fn []
                               (do! message-bus #(assoc % :selected-conversation id))
                               (scroll-messages-to-bottom))}
             [:div.avatar {:style {:background-image (str "url(img/walter-white.jpg)")}}]
             [:div
              [:div.layout.horizontal
               [:div.flex {:class "conv-title one-line-text"} (misc/format-recipients (:recipients conv))]
               [:div {:class "last-update"} (misc/format-time (:last-update conv))]]
              [:span {:class "last-message one-line-text"} (-> conv :messages last misc/msg-text)]]]))

(rum/defc message
          [message-bus msg idx state]
          (let [prev-msg (get-in state [:conversations (:selected-conversation state) :messages (dec idx)])]
            ;;TODO: Message grouping will need to be more specific when group messaging is added
            [:div {:class (str (:type msg) " message-row " (when (= (:type msg) (:type prev-msg)) "grouped"))}
             [:div {:class "message-container"}
              (when (= "sending" (:status msg))
                [:div.loaders.spinner])
              [:div {:class (str "message " (when (= "sending" (:status msg)) "sending"))}
               (:data msg)]
              [:div {:class "timestamp"} (misc/format-time-message (:timestamp msg))]]]))

(defn selected-conv
  [state]
  (get-in state [:conversations (:selected-conversation state)]))

(defn send-message!
  [message-bus state]
  (when-not (str/blank? (:input-value state))
    (let [socket_id (:socket_id state)
          id (str (gensym "msg"))
          conv (selected-conv state)
          value (:input-value state)]
      (do! message-bus (fn [s]
                         (update-in s [:conversations (:selected-conversation state) :messages]
                                    (fn [msgs]
                                      (conj msgs {:type   "sent"
                                                  :data   value
                                                  :status "sending"})))))
      (do! message-bus #(assoc % :input-value ""))
      (scroll-messages-to-bottom)
      (m/send-message! socket_id id conv value))))

(rum/defc root < subscribe-on-mount
          [message-bus state]
          (let [conv (selected-conv state)]
            [:div.layout.vertical
             [:div.nav-bar {:style {:background-color "green"}}]
             [:main.flex
              [:div {:class "horz-center conv-container"}
               [:div.layout.horizontal.fit
                [:div {:class "conv-list"}
                 (map (fn [[id conv]]
                        (conversation-item message-bus state id))
                      (-> state :conversations m/sort-conversations))]
                [:div {:class "flex conv-messages"}
                 [:div.fit
                  [:div {:class "message-list-container"
                         :id    "message-list-container"}
                   (let [messages (:messages conv)]
                     (map-indexed (fn [idx msg]
                                    (message message-bus msg idx state)) messages))]
                  [:div {:class "new-message-container"}
                   [:div.layout.vertical.center-justified {:style {:height "100%"}}
                    [:div.layout.horizontal
                     [:span {:class           "flex"
                             :placeholder     "Send a message"
                             :contentEditable true
                             :on-input        (fn [e]
                                                (do! message-bus #(assoc % :input-value (aget e "target" "outerText"))))
                             :on-key-down     (fn [e]
                                                (when (= 13 (aget e "keyCode"))
                                                  (when-not (aget e "shiftKey")
                                                    (.stopPropagation e)
                                                    (.preventDefault e)
                                                    (send-message! message-bus state))))}
                      (:input-value state)]
                     [:div.layout.vertical.center-justified
                      [:i {:class    "zmdi zmdi-mail-send"
                           :on-click (fn [] (send-message! message-bus state))}]]]]]]]]]]]))