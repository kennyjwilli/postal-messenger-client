(ns postal-messenger.client.views.core
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [cats.labs.lens :as l]
            [cljs-time.core :as t]
            [postal-messenger.client.util.messaging :as m]
            [postal-messenger.client.util.misc :as misc]))

(rum/defc conversation-item
          [message-bus state id]
          (let [conv (get-in state [:conversations id])]
            [:div {:class    (str "conv noselect pointer " (when (= (:selected-conversation state) id) "selected"))
                   :key      (str "conv" id)
                   :on-click (fn [] (do! message-bus #(assoc % :selected-conversation id)))}
             [:div.avatar {:style {:background-image (str "url(img/walter-white.jpg)")}}]
             [:div
              [:div.layout.horizontal
               [:div.flex {:class "conv-title one-line-text"} (misc/format-recipients (:recipients conv))]
               [:div {:class "last-update"} (misc/format-time (:last-update conv))]]
              [:span {:class "last-message one-line-text"} (-> conv :messages last misc/msg-text)]]]))

(rum/defc message
          [message-bus msg]
          [:div {:class (str (:type msg) " message-container")}
           [:div {:class "message"} (:data msg)]])

(rum/defc root
          [message-bus state]
          [:div.layout.vertical
           [:div.nav-bar {:style {:background-color "green"}}]
           [:main.flex
            [:div {:class "horz-center conv-container"}
             [:div.layout.horizontal.fit
              [:div {:class "conv-list"}
               (map (fn [[id conv]]
                      (conversation-item message-bus state id)
                      #_(g/lensed-component (l/in [:conversations id]) conversation-item message-bus state id))
                    (-> state :conversations m/sort-conversations))]
              [:div {:class "flex conv-messages"}
               [:div.fit
                [:div.layout.vertical
                 [:div {:class "message-list-container flex"}
                  (let [messages (get-in state [:conversations (:selected-conversation state) :messages])]
                    (map (fn [msg]
                           (message message-bus msg)) messages))]
                 [:div {:class "new-message-container"}
                  [:div.layout.vertical.center-justified {:style {:height "100%"}}
                   [:div.layout.horizontal
                    [:span {:class           "flex"
                            :placeholder     "Send a message"
                            :contentEditable true
                            :on-input        (fn [e]
                                               (do! message-bus #(assoc % :input-value (aget e "target" "outerText"))))}]
                    [:div.layout.vertical.center-justified
                     [:i {:class "zmdi zmdi-mail-send"}]]]]]]]]]]]])