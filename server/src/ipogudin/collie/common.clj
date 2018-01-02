(ns ipogudin.collie.common)

(defn deep-merge [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (into [] (concat x y))
                      (list? y) (concat x y)
                      :else y))
              a b))