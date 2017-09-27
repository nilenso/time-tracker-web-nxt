(ns time-tracker-web-nxt.config
  (:require
   [taoensso.timbre :as timbre]
   [time-tracker-web-nxt.env-vars :as env]))

(def debug?
  ^boolean goog.DEBUG)

;; Set the logging level
;; Possible values are #{:trace :debug :info :warn :error :fatal :report}
(timbre/set-level! (keyword (:log-level env/env)))
