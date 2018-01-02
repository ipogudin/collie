(ns ipogudin.collie.protocol)

(defrecord Command [id code values])

(defrecord Request [id commands])