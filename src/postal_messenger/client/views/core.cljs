(ns postal-messenger.client.views.core
  (:require [rum.core :as rum]
            [griebenschmalz.core :refer [do!] :as g]
            [cats.labs.lens :as l]
            [cljs-time.core :as t]
            [postal-messenger.client.util.messaging :as m]
            [postal-messenger.client.util.misc :as misc]
            [clojure.string :as str]
            [postal-messenger.client.util.http :as http]
            [postal-messenger.client.util.notification :as notif]
            [promesa.core :as p]
            [beicon.core :as s]
            [pusher.core :as pusher]
            [datascript.core :as d]
            [datascript.transit :as dt]))

;;====================================
;; HELPERS
;;====================================

(defn select-conv!
  [id state message-bus]
  (let [conv (get-in state [:conversations id])]
    (when (= (:status conv) :empty)
      (m/get-conversation! (:socket_id state) (:thread_id conv))))
  (do! message-bus (fn [s]
                     (assoc s :selected-conversation id))))

(defmulti handle-event (fn [evt]
                         (:type evt)))

(defmethod handle-event "add-message"
  [event _ message-bus]
  (let [message (-> event :data misc/normalize-message)
        recipients (:recipients message)
        id (misc/conversation-id recipients)]
    (do! message-bus (fn [s]
                       (let [s (update-in s [:conversations id :messages] #(conj (vec %) message))
                             s (assoc-in s [:conversations id :recipients] recipients)]
                         (when (notif/has-permission?)
                           (notif/notify (misc/format-recipients (:db s) recipients)
                                         {:body     (misc/msg-text message)
                                          :on-click (fn [n]
                                                      (notif/close n)
                                                      (.focus js/window)
                                                      (select-conv! id s message-bus))}))
                         (assoc-in s [:conversations id :last-update] (:date message)))))))

(defmethod handle-event "message-sent"
  [event _ message-bus]
  (do! message-bus (fn [s]
                     (let [message (-> event :data misc/normalize-message)
                           id (misc/conversation-id (:recipients message))
                           idx (:idx message)
                           s (update-in s [:conversations id :messages idx] (fn [msg]
                                                                              (assoc msg :status "sent"
                                                                                         :date (:date message))))]
                       (assoc-in s [:conversations id :last-update] (:date message))))))

(defmethod handle-event "get-contacts"
  [event db message-bus]
  (do! message-bus (fn [s]
                     (let [ids (d/q '[:find [?e ...] :where [?e :contact/name]] db)
                           retract-tx (map (fn [id] [:db.fn/retractEntity id]) ids)
                           db (d/db-with db retract-tx)]
                       (assoc s :db (d/db-with db (misc/contacts-list->tx (:data event))))))))

(defmethod handle-event "get-conversations"
  [event _ message-bus]
  (do! message-bus (fn [s]
                     (let [convs (misc/conv-list->map (:data event))
                           #_sorted #_(misc/sort-conversations convs)
                           #_newest #_(-> sorted vec ffirst)]
                       (assoc s :conversations convs)))))

(defmethod handle-event "get-conversation"
  [event _ message-bus]
  (let [data (:data event)]
    (do! message-bus (fn [s]
                       (update-in s [:conversations (misc/conversation-id (:recipients data))]
                                  (fn [conv]
                                    (assoc conv :messages (mapv misc/normalize-message (:messages data))
                                                :status :complete)))))))

(defn- event-handler
  [event db message-bus]
  (println "INCOMING" event)
  (when (= (:dest event) "client")
    (handle-event event db message-bus)))

(defn- connect-pusher
  [channel api-key db message-bus]
  (let [p (pusher/pusher api-key {:authEndpoint "/api/pusher-auth"
                                  :auth         {:headers (misc/jwt-headers)}})
        channel (pusher/channel p channel)
        pusher-bus (pusher/subscribe channel "messages" {:parse-fn #(js->clj % :keywordize-keys true)})
        #_stream #_(-> pusher-bus (s/filter #(= (:dest %) :client)))]
    (pusher/on-connected p (fn []
                             (let [socket_id (pusher/socket-id p)]
                               (s/on-value pusher-bus #(event-handler (misc/normalize-event %) db message-bus))
                               ;; TODO: Store contacts in localstorage
                               (m/get-contacts! socket_id)
                               (m/get-conversations! socket_id)
                               (do! message-bus (fn [s]
                                                  (assoc s :socket_id socket_id))))))))

(defn selected-conv
  [state]
  (get-in state [:conversations (:selected-conversation state)]))

(defn input-value
  [state]
  (get-in state [:conversations (:selected-conversation state) :input-value]))

(defn update-input-value
  [v state]
  (assoc-in state [:conversations (:selected-conversation state) :input-value] v))

(defn send-message!
  [message-bus state text]
  (let [socket_id (:socket_id state)
        conv (selected-conv state)
        idx (-> conv :messages count)]
    (do! message-bus (fn [s]
                       (let [s (update-in s [:conversations (:selected-conversation state) :messages]
                                          (fn [msgs]
                                            (conj msgs {:type   "sent"
                                                        :text   text
                                                        :status "sending"})))]
                         (assoc-in s [:conversations (:selected-conversation state) :last-update] (t/time-now)))))
    (m/send-message! socket_id idx conv text)))

;;====================================
;; Mixins
;;====================================

(defn- should-scroll? [node]
  (<=
    (- (.-scrollHeight node) (.-scrollTop node) (.-offsetHeight node))
    0))

(def sticky-mixin
  {:will-update
   (fn [state]
     (let [node (js/ReactDOM.findDOMNode (:rum/react-component state))]
       (assoc state ::sticky? (should-scroll? node))))
   :did-update
   (fn [state]
     (when (::sticky? state)
       (let [node (js/ReactDOM.findDOMNode (:rum/react-component state))]
         (set! (.-scrollTop node) (.-scrollHeight node))))
     state)})

(def subscribe-on-mount
  {:did-mount (fn [state]
                (let [message-bus (-> state :rum/args first)
                      s (-> state :rum/args second)]
                  (p/then (http/get! "/api/pusher")
                          (fn [{body :body}]
                            (connect-pusher (:message-channel body) (:api-key body) (:db s) message-bus)))
                  state))})


;;====================================
;; UI
;;====================================

(rum/defc spinner < rum/static
          []
          [:div.loaders.spinner {:style {:width  "3em"
                                         :height "3em"}}])

(rum/defcs compose-pane < (rum/local nil)
           [s message-bus state]
           (let [local (:rum/local s)
                 conv (selected-conv state)]
             [:div {:class "new-message-container"}
              [:textarea {:id          "compose-area"
                          :disabled    (or (nil? conv) (empty? (:messages conv)))
                          :autofocus   true
                          #_:on-input    #_(fn [e]
                                             ;; This is for an auto expanding textarea
                                             (let [elem (aget e "target")]

                                               (doto elem
                                                 (aset "style" "height" "")
                                                 (aset "style" "height" (let [scroll (aget elem "scrollHeight")
                                                                              h (aget elem "clientHeight")
                                                                              _ (println scroll h)
                                                                              calc (js/Math.min scroll 85)]
                                                                          (str calc "px"))))
                                               ;(reset! local (aget elem "style" "height"))
                                               #_(do! message-bus #(assoc % :compose-pane-h (aget elem "parentNode" "offsetHeight")))))
                          :on-key-down (fn [e]
                                         (let [value (aget e "target" "value")]
                                           (when (and (= 13 (aget e "keyCode"))
                                                      (not (aget e "shiftKey")))
                                             (.stopPropagation e)
                                             (.preventDefault e)
                                             (when (not (str/blank? value))
                                               (send-message! message-bus state value)
                                               (aset (aget e "target") "value" "")))))}]]))

(rum/defc conversation-item
          [message-bus state id]
          (let [conv (get-in state [:conversations id])]
            [:div {:class    (str "conv noselect pointer " (when (= (:selected-conversation state) id) "selected"))
                   :on-click (fn []
                               (select-conv! id state message-bus))}
             #_[:div.avatar {:style {:background-image (str "url(img/walter-white.jpg)")}}]
             [:div {:class "contact-icon no-photo"}
              (let [c (misc/contact-from-number (first (:recipients conv)) (:db state))]
                (if c
                  (str/join "" (take 2 (misc/initials (:contact/name c))))
                  "#"))]
             [:div
              [:div.layout.horizontal
               [:div.flex {:class "conv-title one-line-text"} (misc/format-recipients (:db state) (:recipients conv))]
               [:div {:class "last-update"} (misc/format-time (:last-update conv))]]
              [:span {:class "last-message one-line-text"} (or (-> conv :messages last misc/msg-text) (:snippet conv) "")]]]))

(rum/defc conversation-list-pane
          [message-bus state]
          [:div {:class "conv-list"}
           (map (fn [[id conv]]
                  (rum/with-key (conversation-item message-bus state id) id))
                (-> state :conversations misc/sort-conversations))])

(rum/defc message
          [message-bus msg idx state]
          (let [prev-msg (get-in state [:conversations (:selected-conversation state) :messages (dec idx)])]
            ;;TODO: Message grouping will need to be more specific when group messaging is added
            [:div {:class (str (:type msg) " message-row " (when (= (:type msg) (:type prev-msg)) "grouped"))}
             [:div {:class "message-container"}
              (when (= "sending" (:status msg))
                [:div.loaders.spinner])
              [:div {:class (str "message " (when (= "sending" (:status msg)) "sending"))}
               (:text msg)]
              [:div {:class "timestamp"}
               (let [t (:date msg)]
                 (if t
                   (misc/format-time-message t)
                   "Sending..."))]]]))

(rum/defc chat-pane < sticky-mixin
          [message-bus state]
          (let [conv (selected-conv state)]
            [:div {:class "message-list-container"
                   :id    "message-list-container"}
             (if (= (:status conv) :complete)
               (let [messages (:messages conv)]
                 (map-indexed (fn [idx msg]
                                (rum/with-key (message message-bus msg idx state) idx)) messages))
               [:div.layout.vertical.center-justified {:style {:height "100%"}}
                [:div.layout.horizontal.center-justified
                 (if conv
                   (spinner)
                   [:span "Select a conversation :)"])]])]))

(rum/defc messenger-pane
          [message-bus state]
          [:div.layout.horizontal.fit
           (conversation-list-pane message-bus state)
           [:div {:class "flex conv-messages"}
            [:div.fit
             (chat-pane message-bus state)
             (compose-pane message-bus state)]]])

(rum/defc loading-messenger-pane
          []
          [:div.layout.vertical.center-justified {:style {:height "100%"}}
           [:div.layout.horizontal.center-justified
            [:div.layout.vertical
             [:div.layout.horizontal.center-justified
              (spinner)]
             [:span "Loading your messages"]]]])

(rum/defcs root < (rum/local true :show-notif-req?)
           [cstate message-bus state]
           [:div.layout.vertical
            (let [show? (:show-notif-req? cstate)]
              (when (and @show? (notif/support-notifications?) (not (notif/has-permission?)))
                [:div {:class "request-permission layout vertical center-justified"}
                 [:div.content
                  [:div.layout.horizontal
                   [:div.flex
                    [:span "Enable desktop notifications by clicking "]
                    [:a {:on-click #(notif/request-permission (fn [c]
                                                                (reset! show? false)))} "here"]]
                   [:div.close {:on-click #(reset! show? false)} "X"]]]]))
            [:div.nav-bar {:style {:background-color "green"}}
             [:div.layout.horizontal {:style {:height "100%"}}
              [:div.flex.layout.vertical.center-justified
               [:span.title "Postal Messenger"]]
              [:div.layout.vertical.center-justified
               [:div.avatar {:style {:background-image (str "url(img/dexter.jpg)")}}]]]]
            [:main.flex
             [:div {:class "horz-center conv-container"}
              (if (:conversations state)
                (messenger-pane message-bus state)
                (loading-messenger-pane))]]])