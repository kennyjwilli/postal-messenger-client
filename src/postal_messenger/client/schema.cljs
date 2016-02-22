(ns postal-messenger.client.schema
  (:require [provisdom.datomic-helpers.core :as dh]))

(def number-schema
  {:number/number :db.type/string
   :number/type   :db.type/string})

(def contact-schema
  {:contact/name    :db.type/string
   :contact/numbers [number-schema]})

(def schema (dh/to-schema (dh/to-schema-transaction contact-schema) #{}))