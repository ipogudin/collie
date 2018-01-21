(ns ipogudin.collie.common
  (:require [com.rpl.specter :refer [recursive-path cond-path ALL STAY]]))

; utils for working with data structures
(defn deep-merge
  ([a b]
    (merge-with (fn [x y]
                  (cond (map? y) (deep-merge x y)
                        (vector? y) (into [] (concat x y))
                        (list? y) (concat x y)
                        :else y))
                a b))
  ([a b & others]
   (let [ab (deep-merge a b)
         [c & t] others]
     (if (nil? c)
       ab
       (apply deep-merge (into [ab c] t))))))

(defn remove-ns-from-keyword
  "Removes namespace from a keyword.
  If it is not a keyword returns an unmodified value."
  [v]
  (if
    (keyword? v)
    (keyword (name v))
    v))

; specter paths/navigators

(def ALL-OBJECTS
  "A recursive path to traverse whole objects in nested data structure
  (including keys and values from maps)."
  (recursive-path [] p
                  (cond-path
                    seq? [ALL p]
                    vector? [ALL p]
                    map? [ALL p]
                    some? STAY)))