(ns time-tracker-web-nxt.utils
  (:require
   [cljs-time.coerce :as tcoerce]
   [cljs-time.core :as tcore]
   [cljs-time.format :as tformat]
   [goog.string :as gs]
   [goog.string.format]))

(defn ->seconds [hh mm ss]
  (+ (* 60 60 hh) (* 60 mm) ss))

(defn ->hh-mm-ss [duration]
  (let [hours (quot duration (* 60 60))
        minutes (- (quot duration 60) (* hours 60))
        seconds (- duration (* hours 60 60) (* minutes 60))]
    {:hh hours :mm minutes :ss seconds}))

(defn format-time [elapsed-hh elapsed-mm elapsed-ss]
  (gs/format "%02d:%02d:%02d" elapsed-hh elapsed-mm elapsed-ss))

;; Note:
;; We're doing redundant time conversions for the most common case
;; i.e. today's date. One way to avoid that would have been to
;; compare the timer-date arg with current DateTime. That is always
;; going to differ as timer-date, being selected from a DatePicker
;; doesn'have HH:mm:ss parts.
;; TODO:
;; We might be able to avoid some of this by getting the midnight corresponding
;; to now-datetime and comparing it with timer-datetime.
(defn timer-created-time
  "Takes a date of the form 'Tue Oct 10 2017 11:30:21 GMT+0530 (IST)'
  and returns a corresponding Unix epoch"
  [timer-date now-datetime]
  (let [timer-format "E MMM dd yyyy HH:mm:ss"
        ;; Timer date in db is of the form "Tue Oct 10 2017 11:30:21 GMT+0530 (IST)"
        ;; We choose a format to extract the date in UTC midnight
        ;; To match it with formatter we have to drop the last 15 chars
        timer-datetime (tformat/parse
                        (tformat/formatter timer-format)
                        (clojure.string/join (drop-last 15 timer-date)))
        ;; Timer date stores 00:00:00 for HH:mm:ss
        ;; so we need to advance it by a Period corresponding to now
        created-datetime (tcore/plus
                          timer-datetime
                          (tcore/hours (tcore/hour now-datetime))
                          (tcore/minutes (tcore/minute now-datetime))
                          (tcore/seconds (tcore/second now-datetime)))]
    (tcoerce/to-epoch created-datetime)))
