(ns ipogudin.collie.dev.schema
  (:require [ipogudin.collie.schema :as schema]))

(def schema-description
  [
   {::schema/name :manufacturers
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}]}
   {::schema/name :cars
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}
                   {::schema/name :manufacturer
                    ::schema/field-type ::schema/one-to-one}]}
   ])

(def schema (schema/schema-seq-to-map schema-description))
