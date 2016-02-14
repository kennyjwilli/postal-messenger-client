(ns postal-messenger.client.util.notification)

(def notification (aget js/window "Notification"))

(defn support-notifications?
  []
  (some? notification))

(defn request-permission
  [callback]
  (.. notification requestPermission (then (fn [result]
                                             (callback (keyword result))))))

(defn has-permission?
  []
  (= (aget notification "permission") "granted"))

(defn close
  [notif]
  (.close notif))

(defn notify
  ([title] (notify title {}))
  ([title {:keys [timeout on-click on-error] :or {timeout 4000} :as opts}]
   (let [n (js/Notification. title (clj->js opts))]
     (aset n "onclick" (partial on-click n))
     (aset n "onerror" (partial on-error n))
     (js/setTimeout (.. n -close (bind n)) timeout)
     n)))