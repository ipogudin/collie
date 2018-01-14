(ns ipogudin.collie.common)

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