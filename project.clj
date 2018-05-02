(defproject time-tracker-web-nxt "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.908"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.5"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [nilenso/wscljs "0.1.1"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 [cljs-pikaday "0.1.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [funcool/hodgepodge "0.1.4"]
                 [day8.re-frame/test "0.1.5"]
                 [bidi "2.1.2"]
                 [kibu/pushy "0.3.8"]
                 [cljsjs/toastr "2.1.2-0"]
                 [re-frame-datatable "0.6.0"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :min-lein-version "2.5.3"

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs   ["resources/public/css"]
             :nrepl-port 7888}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
                   [com.cemerick/piggieback "0.2.2"]
                   [figwheel-sidecar "0.5.13"]
                   [pjstadig/humane-test-output "0.8.3"]
                   [day8.re-frame/re-frame-10x "0.2.1"]]

    :plugins [[lein-figwheel "0.5.13"]
              [lein-doo "0.1.7"]]
    }}

  :aliases {"prod" ["do" "clean" ["cljsbuild" "once" "min"]]}
  :doo {:build "test"
        :alias {:default [:phantom :once]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "time-tracker-web-nxt.core/mount-root"}
     :compiler     {:main                 time-tracker-web-nxt.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :closure-defines      {time-tracker-web-nxt.config.debug? true
                                           "re_frame.trace.trace_enabled_QMARK_" true}
                    :asset-path           "/js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            time-tracker-web-nxt.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :whitespace
                    :closure-defines {time-tracker-web-nxt.config.debug? false}
                    :pretty-print    false}}

    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:main          time-tracker-web-nxt.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none
                    :process-shim  false}}
    ]}
  )
