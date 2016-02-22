(ns postal-messenger.client.util.misc
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [cljs-time.core :as t]
            [cljs-time.format :as ft]
            [postal-messenger.client.util.cookie :as cookie]
            [datascript.core :as d])
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
  (:data msg))

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
          :contact/numbers (phone-numbers->tx (:phoneNumbers m))}) contacts-list))