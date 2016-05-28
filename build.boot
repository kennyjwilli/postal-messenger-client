(def project 'postal/client)
(def version "0.1.0-SNAPSHOT")

(set-env!
  :resource-paths #{"src" "test" "scss"}
  :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                  [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                  [adzerk/boot-reload "0.4.8" :scope "test"]
                  [pandeiro/boot-http "0.7.3" :scope "test"]
                  [cljsjs/boot-cljsjs "0.5.1" :scope "test"]
                  [provisdom/boot-tasks "0.6.0" :scope "test"]
                  [mathias/boot-sassc "0.1.5"]
                  ;; project deps
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [rum "0.8.3"]
                  [datascript "0.15.0"]
                  [datascript-transit "0.2.0"]
                  [funcool/postal "0.7.0"]
                  [funcool/beicon "1.4.0"]
                  [funcool/promesa "1.2.0"]
                  [funcool/cats "1.2.1"]
                  [funcool/httpurr "0.6.0"]
                  [cljs-http "0.1.40"]
                  [griebenschmalz "0.5.0-SNAPSHOT"]
                  [provisdom/datomic-helpers "2.3.1"]
                  [com.andrewmcveigh/cljs-time "0.4.0"]
                  [com.cemerick/url "0.1.1"]
                  [pusher "0.1.1"]]
  :compiler-options {:compiler-stats true})

(require
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer :all]
  '[provisdom.boot-tasks.core :refer :all]
  '[pandeiro.boot-http :refer :all]
  '[cljsjs.boot-cljsjs :refer :all]
  '[mathias.boot-sassc :refer :all])

(task-options!
  pom {:project     project
       :version     version
       :description "The Postal Messenger web client"
       :url         "https://github.com/kennyjwilli/postal-messenger-client"
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask web-dev
         "Developer workflow for web-component UX."
         []
         (comp
           (asset-paths :asset-paths #{"html" "bower_components"})
           (serve :dir "target/")
           (watch)
           (cljs-repl)
           (checkout :dependencies [['pusher "0.1.0-SNAPSHOT"]])
           (speak)
           (reload)
           (sass :sass-file "main.scss"
                 :output-dir "styles"
                 :line-numbers true
                 :source-maps true)
           (cljs)))

(deftask prod-build
         []
         (comp
           (asset-paths :asset-paths #{"html" "bower_components"})
           (sass :sass-file "main.scss"
                 :output-dir "styles"
                 :line-numbers false
                 :source-maps false
                 :output-style "compressed")
           (cljs :optimizations :advanced)))