(ns time-tracker-web-nxt.config
  (:require
   [taoensso.timbre :as timbre]))

;; Switch off debug mode by default
;; Will be overridden by setting `:closure-defines` in your compiler
;; options for the appropriate build in project.clj
(goog-define ^boolean debug? false)

(def dev
  {:client-id "673127655477-iidn2bttouhpvh2a4l6l8e7cp1pr42uo.apps.googleusercontent.com" 
   :scope "profile email"
   :conn-url "ws://localhost:8000/api/timers/ws-connect/"
   :log-level "debug"})

(def prod
  {:client-id "163789226845-gfp9na27iac63lnrb81hsdkvf5log85i.apps.googleusercontent.com"
   :scope "profile email"
   :conn-url "ws://time.nilenso.com/api/timers/ws-connect/"
   :log-level "info"})

(def env (if debug? dev prod))

;; Set the logging level
;; Possible values are #{:trace :debug :info :warn :error :fatal :report}
(timbre/set-level! (keyword (:log-level env)))
