(ns ipogudin.collie.entity
  (:require [clojure.spec.alpha :as s]))

(s/def ::type keyword?)

(s/def ::entity (s/keys :req [::type]))