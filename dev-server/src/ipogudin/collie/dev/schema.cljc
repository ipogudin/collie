(ns ipogudin.collie.dev.schema
  (:require [ipogudin.collie.schema :as schema]))

(def schema-description
  [
   {::schema/name :currency
    ::schema/ui {
                 ::schema/title "Currency"
                 ::schema/show-fn :name
                 }
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string
                    ::schema/default ::schema/empty}]}
   {::schema/name :manufacturers
    ::schema/ui {
                   ::schema/title "Manufacturers"
                   ::schema/show-fn :name
                   }
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}
                   {::schema/name :cars
                    ::schema/field-type ::schema/one-to-many
                    ::schema/related-entity :cars
                    ::schema/related-entity-field :manufacturer
                    ::schema/ui {
                                 ::schema/title "Cars"
                                 ::schema/editable false
                                 ::schema/preview-text-length 20
                                 }}]}
   {::schema/name :engine_types
    ::schema/ui {
                 ::schema/show-fn :name
                 }
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true}
                   {::schema/name :code
                    ::schema/field-type ::schema/string}
                   {::schema/name :name
                    ::schema/field-type ::schema/string}]}
   {::schema/name :cars
    ::schema/ui {
                 ::schema/show-fn :name
                 }
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true
                    ::schema/ui {
                                 ::schema/hidden true
                                 }}
                   {::schema/name :visible
                    ::schema/field-type ::schema/boolean
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Visible"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :announced
                    ::schema/field-type ::schema/date
                    ::schema/ts-format "yyyy-MM-dd"
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Announced"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :produced
                    ::schema/field-type ::schema/timestamp
                    ::schema/ts-format "yyyy-MM-dd HH:mm:ss"
                    ::schema/tz-disabled true
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Released"
                                 ::schema/min-width 200
                                 }}
                   {::schema/name :name
                    ::schema/field-type ::schema/string
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/max-length 16
                    ::schema/ui {
                                 ::schema/title "Name"
                                 ::schema/min-width 200
                                 }}
                   {::schema/name :model
                    ::schema/field-type ::schema/string
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Model"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :description
                    ::schema/field-type ::schema/string
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Description"
                                 ::schema/preview-text-length 20
                                 ::schema/min-width 250
                                 }}
                   {::schema/name :engine_type
                    ::schema/field-type ::schema/one-to-one
                    ::schema/related-entity :engine_types
                    ::schema/related-entity-field :code
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Engine Type"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :manufacturer
                    ::schema/field-type ::schema/one-to-one
                    ::schema/related-entity :manufacturers
                    ::schema/related-entity-field :id
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Manufacturers"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :price
                    ::schema/precision 10
                    ::schema/scale 2
                    ::schema/field-type ::schema/decimal
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Price"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :drive_wheels
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Drive Wheels"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :width
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Width"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :length
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Length"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :height
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Height"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :transmission_speed
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Transmission Speed"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :cylinders
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Cylinders"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :min_kerb_weight
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Min Kerb Weight"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :max_kerb_weight
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Max Kerb Weight"
                                 ::schema/min-width 150
                                 }}
                   {::schema/name :gross_weight_limit
                    ::schema/field-type ::schema/int
                    ::schema/nullable true
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/title "Gross Weight Limit"
                                 ::schema/min-width 150
                                 }}]}
   {::schema/name :showrooms
    ::schema/fields
                  [
                   {::schema/name :id
                    ::schema/field-type ::schema/serial
                    ::schema/primary-key true
                    ::schema/ui {
                                 ::schema/default-order :desc
                                 }}
                   {::schema/name :name
                    ::schema/field-type ::schema/string
                    ::schema/default ::schema/empty}
                   {::schema/name :cars
                    ::schema/field-type ::schema/many-to-many
                    ::schema/related-entity :cars
                    ::schema/related-entity-field :id
                    ::schema/relation :showrooms_to_cars
                    ::schema/left :showroom
                    ::schema/right :car
                    ::schema/default ::schema/empty
                    ::schema/ui {
                                 ::schema/selector-size 10
                                 }}]}
   ])

(def schema (schema/schema-seq-to-map schema-description))
