(ns postal-messenger.client.util.misc
  (:require [clojure.string :as str]
            [postal-messenger.client.util.cookie :as cookie]))

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