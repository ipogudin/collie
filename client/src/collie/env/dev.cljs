(ns ^:figwheel-no-load collie.env.dev
  (:require [collie.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:7000/figwheel-ws"
  :jsload-callback core/init!)

(core/init!)
