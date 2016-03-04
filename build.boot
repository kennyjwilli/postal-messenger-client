(set-env!
  :source-paths #{"src" "test"}
  :resource-paths #{"src" "test" "scss" "bower_components"}
  :wagons '[[s3-wagon-private "1.1.2"]]
  :repositories [["clojars" "http://clojars.org/repo/"]
                 ["maven-central" "http://repo1.maven.org/maven2/"]]
  :dependencies '[[org.clojure/clojure "1.7.0" :scope "provided"]
                  [org.clojure/clojurescript "1.7.170"]
                  [adzerk/boot-cljs "1.7.170-3" :scope "test"]
                  [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                  [adzerk/boot-reload "0.4.2" :scope "test"]
                  [pandeiro/boot-http "0.7.0" :scope "test"]
                  [cljsjs/boot-cljsjs "0.5.0" :scope "test"]
                  [provisdom/boot-tasks "0.4.0" :scope "test"]
                  [mathias/boot-sassc "0.1.5"]]
  :compiler-options {:compiler-stats true})

(require
  '[adzerk.boot-cljs :refer :all]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload :refer :all]
  '[provisdom.boot-tasks :refer :all]
  '[pandeiro.boot-http :refer :all]
  '[cljsjs.boot-cljsjs :refer :all]
  '[mathias.boot-sassc :refer :all])

(set-project-deps!)

(default-task-options!)

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