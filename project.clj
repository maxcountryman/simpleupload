(defproject simpleupload "0.0.1"
  :description "Simple email uploader."
  :url "https://github.com/maxcountryman/simpleupload"
  :license {:name "BSD"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[clasp "0.0.1"]
                 [ring/ring-core "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.codec "0.1.0"]]
  :plugins [[lein-ring "0.8.3"]]
  :ring {:handler simpleupload.app/create-app
         :adapter {:port 8080}})
