(ns app.site
  "Builds the site into dist/: a page per meetup from data/, plus static/ as
  it is. Run from the project's root.

  BASE_PATH is where the site is served from, like /meetups for GitHub Pages'
  https://<owner>.github.io/meetups/. The deploy workflow sets it."
  (:require [app.meetups :as meetups]
            [app.views.helpers :as helpers]
            [app.views.home :as home]
            [app.views.layout :as layout]
            [app.views.meetup :as meetup]
            [app.views.paths :as paths]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (com.sun.net.httpserver HttpServer SimpleFileServer)
           (java.io File)
           (java.net InetAddress InetSocketAddress)))

(def out-dir "dist")

(defn- base-path
  "BASE_PATH without a trailing slash: \"\" or like \"/meetups\"."
  []
  (let [path (or (System/getenv "BASE_PATH") "")]
    (when-not (re-matches #"(/[\w.-]+)*/?" path)
      (throw (ex-info (str "BASE_PATH should be empty or a path like /meetups, not " (pr-str path)) {})))
    (str/replace path #"/$" "")))

(defn- github-repo
  "On GitHub Actions, the repository and branch being built, so the organizer
  pages link to where the site really comes from."
  []
  (when-let [repo (System/getenv "GITHUB_REPOSITORY")]
    {:repo-url    (str (System/getenv "GITHUB_SERVER_URL") "/" repo)
     :repo-branch (System/getenv "GITHUB_REF_NAME")}))

(defn read-data
  "Everything the pages are made of, from data/."
  []
  {:site      (merge (edn/read-string (slurp "data/site.edn")) (github-repo))
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
  (binding [helpers/*base-path* (base-path)]
    (let [data (read-data)]
      (warn-about-placeholders (:site data))
      (delete-dir! out-dir)
      (copy-dir! "static" out-dir)
      (doseq [[path html] (pages data)]
        (let [file (io/file out-dir path)]
          (io/make-parents file)
          (spit file html)))
      (println "Built" (count (:meetups data)) "meetups into" (str out-dir "/")
               (if (seq helpers/*base-path*) (str "for " helpers/*base-path* "/") "")))))

(defn serve
  "Builds the site and serves it on http://localhost:8080, or `:port`, under
  BASE_PATH like GitHub Pages does. Build again to see changes."
  [{:keys [port] :or {port 8080}}]
  (build nil)
  (let [base   (base-path)
        server (HttpServer/create (InetSocketAddress. (InetAddress/getLoopbackAddress) (int port)) 0)]
    (.createContext server (str base "/")
                    (SimpleFileServer/createFileHandler (.toPath (.getAbsoluteFile (io/file out-dir)))))
    (.start server)
    (println (str "Serving on http://localhost:" port base "/"))
    @(promise)))
