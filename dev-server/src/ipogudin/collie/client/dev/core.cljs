(ns ^:figwheel-no-load ipogudin.collie.client.dev.core
  (:require [ipogudin.collie.client.dev.app :as app]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:9449/figwheel-ws"
  :jsload-callback app/init!)

(app/init!)
