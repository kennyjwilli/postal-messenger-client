(ns postal-messenger.client.util.misc
  (:require [clojure.string :as str]
            [cljs-time.core :as t]
            [cljs-time.format :as ft]
            [postal-messenger.client.util.cookie :as cookie]
            [datascript.core :as d]
            [cljs-time.format :as fmt])
  (:import
    goog.date.Date
    goog.date.DateTime
    goog.date.UtcDateTime))

(def jwt-cookie-name "POSTAL_JWT")

(defn get-jwt
  []
  (cookie/get-cookie jwt-cookie-name))

(defn set-jwt!
  [jwt]
  (cookie/set-cookie! jwt-cookie-name jwt))

(defn jwt-headers
  []
  {"Authorization" (str "Token " (get-jwt))})

(defn parse-json
  [json]
  (-> json js/JSON.parse (js->clj :keywordize-keys true)))

(defn get-url
  []
  (aget js/window "location" "href"))

(defn url-origin
  []
  (.-origin js/location))

(defn http-redirect
  [url]
  (.. js/window -location (replace url)))

(defn scroll-to-bottom
  [elem]
  (aset elem "scrollTop" (aget elem "scrollHeight")))

(defn contact-from-number
  [number db]
  (d/entity db
            (d/q '[:find ?c .
                   :in $ ?num
                   :where
                   [?n :number/number ?num]
                   [?c :contact/numbers ?n]] db number)))

(defn initials
  [name]
  (let [split (str/split name " ")]
    (map first split)))

(defn conversation-id
  "Returns a unique id for a given set of recipients."
  [recipients]
  (hash (sort-by hash recipients)))

(defn time-comparator
  "Sort time by closest time first"
  [x y]
  (cond
    (t/equal? x y) 0
    (t/after? x y) -1
    :default 1))

(defn sort-conversations
  "Sorts conversations by the newest conversation first"
  [convs]
  (into (sorted-map-by (fn [x y]
                         (time-comparator (get-in convs [x :last-update])
                                          (get-in convs [y :last-update])))) convs))

(defn parse-time
  [time-str]
  (fmt/parse (:date-hour-minute-second-ms fmt/formatters) time-str))

(defn normalize-message
  [msg]
  (update msg :date (fn [date]
                      (when date
                        (parse-time date)))))

(defn format-recipients
  [db recip-list]
  (str/join ", " (map (fn [num]
                        (let [contact (contact-from-number num db)]
                          (if contact
                            (:contact/name contact)
                            ;; TODO: format phone number
                            num))) recip-list)))

;;TODO: Will need to handle MMS
(defn msg-text
  "Returns a text form of the message"
  [msg]
  (:text msg))

(defn today-at
  ([hours minutes seconds millis]
   (let [midnight (doto (goog.date.Date.) (.setTime (t/*ms-fn*)))]
     (doto (goog.date.DateTime. 0)
       (.setYear (.getYear midnight))
       (.setMonth (.getMonth midnight))
       (.setDate (.getDate midnight))
       (.setHours hours)
       (.setMinutes minutes)
       (.setSeconds seconds)
       (.setMilliseconds millis))))
  ([hours minutes seconds]
   (today-at hours minutes seconds 0))
  ([hours minutes]
   (today-at hours minutes 0)))

(defn end-of-today
  []
  (today-at 23 59 59 999))

(defn within-today?
  "Returns if the given date is within today"
  [date]
  (t/within? (today-at 0 0) (end-of-today) date))

(defn within-week?
  "Returns if the given date is within a week from the end of today"
  [date]
  (let [end (end-of-today)]
    (t/within? (t/minus end (t/weeks 1)) end date)))

(def days-of-week-short ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"])

(defn format-time
  [time]
  (letfn [(parse [fmt] (ft/unparse fmt time))]
    (cond
      (within-today? time) (str/upper-case (parse (ft/formatter "h:mm a")))
      (within-week? time) (let [d (js/parseInt (ft/unparse (ft/formatter "e") time))]
                            (days-of-week-short d))
      :default (parse (ft/formatter "MM/dd/yyyy")))))

(defn format-time-message
  [time]
  (letfn [(parse [fmt] (ft/unparse fmt time))
          (time-format [] (str/upper-case (parse (ft/formatter "h:mm a"))))]
    (cond
      (within-today? time) (time-format)
      (within-week? time) (let [d (js/parseInt (ft/unparse (ft/formatter "e") time))]
                            (str (days-of-week-short d) " " (time-format)))
      :default (str (parse (ft/formatter "MM/dd/yyyy")) " " (time-format)))))

(defn phone-numbers->tx
  [nums]
  (map (fn [m]
         {:number/number (:number m)
          :number/type   (:type m)}) nums))

(defn contacts-list->tx
  [contacts-list]
  (map (fn [m]
         {:contact/name    (:name m)
          :contact/numbers (phone-numbers->tx (:numbers m))}) contacts-list))

(defn conv-list->map
  [convs]
  (apply hash-map
         (mapcat (fn [conv]
                   (let [recipients [(:address conv)]
                         date (parse-time (:date conv))
                         text (:text conv)
                         thread_id (:thread_id conv)]
                     [(conversation-id recipients) {:recipients  recipients
                                                    :last-update date
                                                    :thread_id   thread_id
                                                    :snippet     text
                                                    :status      :empty
                                                    :messages    []}])) convs)))

(defn normalize
  [structure value]
  (if (sequential? value)
    (mapv (partial normalize structure) value)
    (apply hash-map
           (mapcat (fn [[k v]]
                     (if (contains? structure k)
                       (let [new-k (get structure k)]
                         (if (vector? new-k)
                           [(first new-k) (normalize (second new-k) v)]
                           [new-k v]))
                       [k v])) value))))

(defn normalize-event
  [event]
  (let [s {:d :dest
           :t :type
           :b [:data {:t  :type
                      :th :thread_id
                      :r  :recipients
                      :d  :date
                      :i  :idx
                      :n  :name
                      :a  :address
                      :m  [:messages {:t :type
                                      :r :recipients
                                      :d :date
                                      :b :text}]
                      :p  [:numbers {:t :type
                                     :n :number}]
                      :b  :text}]}]
    (normalize s event)))