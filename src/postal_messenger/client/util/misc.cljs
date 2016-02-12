(ns postal-messenger.client.util.misc
  (:require [clojure.string :as str]
            [cljs-time.core :as t]
            [cljs-time.format :as ft]
            [postal-messenger.client.util.cookie :as cookie])
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

(defn format-recipients
  [recip-list]
  (str/join ", " (map (fn [recip] (str (:first-name recip) " " (:last-name recip))) recip-list)))

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

(defn within-today?
  [date]
  (t/within? (today-at 0 0) (today-at 23 59 59 999) date))

(defn format-time
  [time]
  )