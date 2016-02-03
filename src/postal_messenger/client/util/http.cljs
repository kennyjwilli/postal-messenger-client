(ns postal-messenger.client.util.http
  (:require [clojure.string :as str]
            [httpurr.client.xhr :as http]
            [promesa.core :as p]
            [postal-messenger.client.util.misc :as misc]))

(defn decode-json
  [json]
  (js->clj (js/JSON.parse json) :keywordize-keys true))

(defn encode-json
  [payload]
  (-> payload clj->js js/JSON.stringify))

(defmulti decode-resp (fn [resp] (-> resp (get-in [:headers "Content-Type"]) (str/split ";") first)))

(defmethod decode-resp :default
  [resp]
  resp)

(defmethod decode-resp "application/json"
  [resp]
  (update resp :body decode-json))

(defn get!
  [url]
  (p/then (http/get url {:headers (misc/jwt-headers)}) (fn [v] (decode-resp v))))

(defn post!
  [url body]
  (p/then (http/post url {:headers (merge (misc/jwt-headers) {:content-type "application/json"})
                          :body    (encode-json body)})
          decode-resp))