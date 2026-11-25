(ns app.site
  "Builds the site into dist/: a page per meetup from data/, plus static/ as
  it is. Run from the project's root."
  (:require [app.meetups :as meetups]
            [app.views.home :as home]
            [app.views.layout :as layout]
            [app.views.meetup :as meetup]
            [app.views.paths :as paths]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.sun.net.httpserver SimpleFileServer SimpleFileServer$OutputLevel)
           (java.io File)
           (java.net InetAddress InetSocketAddress)))

(def out-dir "dist")

(defn read-data
  "Everything the pages are made of, from data/."
  []
  {:site      (edn/read-string (slurp "data/site.edn"))
   :meetups   (meetups/read-dir "data/meetups")
   :templates {:meetup (slurp "data/templates/meetup.edn")
               :event  (slurp "data/templates/event.edn")}})

(defn pages
  "Path under dist/ -> HTML."
  [{:keys [site meetups templates]}]
  (into {"index.html"                    (home/page {:site site :meetups meetups})
         "404.html"                      (layout/not-found {:site site})
         "add-your-meetup/index.html"    (paths/add-page site (:meetup templates))
         "sponsorship/index.html"        (paths/sponsorship-page site)
         "update-your-meetup/index.html" (paths/update-page site (:event templates))}
        (for [m meetups]
          [(str "meetups/" (:slug m) "/index.html") (meetup/page m {:site site})])))

(defn- copy-dir! [from to]
  (let [root (.toPath (io/file from))]
    (doseq [^File file (file-seq (io/file from))
            :when (.isFile file)
            :let [target (io/file to (str (.relativize root (.toPath file))))]]
      (io/make-parents target)
      (io/copy file target))))

(defn- delete-dir! [dir]
  (doseq [file (reverse (file-seq (io/file dir)))]
    (io/delete-file file true)))

(defn- warn-about-placeholders [site]
  (when-not (:sponsorship-form-url site)
    (println "No :sponsorship-form-url in data/site.edn, the sponsorship page's button goes nowhere"))
  (when-not (get-in site [:newsletter-form :action])
    (println "No :action for :newsletter-form in data/site.edn, newsletter sign-ups won't be sent anywhere")))

(defn build [_]
  (let [data (read-data)]
    (warn-about-placeholders (:site data))
    (delete-dir! out-dir)
    (copy-dir! "static" out-dir)
    (doseq [[path html] (pages data)]
      (let [file (io/file out-dir path)]
        (io/make-parents file)
        (spit file html)))
    (println "Built" (count (:meetups data)) "meetups into" (str out-dir "/"))))

(defn serve
  "Builds the site and serves it on http://localhost:8080, or `:port`. Build
  again to see changes."
  [{:keys [port] :or {port 8080}}]
  (build nil)
  (.start (SimpleFileServer/createFileServer
           (InetSocketAddress. (InetAddress/getLoopbackAddress) (int port))
           (.toPath (.getAbsoluteFile (io/file out-dir)))
           SimpleFileServer$OutputLevel/INFO))
  (println (str "Serving on http://localhost:" port))
  @(promise))
