(ns postal-messenger.client.util.cookie
  (:require [goog.net.cookies]
            [cljs.reader :as reader]))

(defn get-cookie "Returns the cookie after parsing it with cljs.reader/read-string."
  [k]
  (reader/read-string (or (.get goog.net.cookies (name k)) "nil")))

(defn set-cookie! "Stores the cookie value using pr-str."
  [k v]
  (.set goog.net.cookies (name k) (pr-str v)))

(defn remove-cookie! "Removes a cookie"
  [k]
  (.remove goog.net.cookies (name k)))