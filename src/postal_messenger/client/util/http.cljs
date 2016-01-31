(ns postal-messenger.client.util.http
  (:require [httpurr.client.xhr :as http]
            [promesa.core :as p]
            [postal-messenger.client.util.misc :as misc]))

(defn decode
  [response]
  (update response :body #(js->clj (js/JSON.parse %) :keywordize-keys true)))

(defn get!
  [url]
  (p/then (http/get url {:headers (misc/jwt-headers)}) decode))