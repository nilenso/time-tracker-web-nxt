(ns time-tracker-web-nxt.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [pjstadig.humane-test-output]
            [time-tracker-web-nxt.core-test]))

(doo-tests 'time-tracker-web-nxt.core-test)
