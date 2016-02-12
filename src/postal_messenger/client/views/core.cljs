(ns postal-messenger.client.views.core
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [cats.labs.lens :as l]
            [cljs-time.core :as t]
            [postal-messenger.client.util.messaging :as msg]
            [postal-messenger.client.util.misc :as misc]))

(rum/defc conversation-item
          [message-bus state id]
          [:div {:class "conv noselect pointer"
                 :key   (str "conv" id)}
           [:div.avatar {:style {:background-image (str "url(img/walter-white.jpg)")}}]
           [:div
            [:div.layout.horizontal
             [:div.flex {:class "conv-title one-line-text"} (misc/format-recipients (:recipients state))]
             [:div {:class "last-update"} "10:59 PM"]]
            [:span {:class "last-message one-line-text"} (-> state :messages last misc/msg-text)]]])

(rum/defc root
          [message-bus state]
          [:div.layout.vertical
           [:div.nav-bar {:style {:background-color "green"}}]
           [:main.flex
            [:div {:class "horz-center conv-container"}
             [:div.layout.horizontal.fit
              [:div {:class "conv-list"}
               (map (fn [[id conv]]
                      (g/lensed-component (l/in [:conversations id]) conversation-item message-bus state id))
                    (-> state :conversations msg/sort-conversations))]
              [:div.flex {:style {:background-color "purple"}}]]]]])