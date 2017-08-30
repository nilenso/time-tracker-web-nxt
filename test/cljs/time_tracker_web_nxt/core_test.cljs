(ns time-tracker-web-nxt.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [time-tracker-web-nxt.core :as core]))

(deftest passing-test
  (testing "basic facts of life" (is (= (+ 1 1) 2))))
