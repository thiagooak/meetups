(ns app.meetups
  "Meetups live in data/meetups/<country code>/<meetup>/: meetup.edn says
  what the meetup is, and a <yyyy-MM-dd>.edn for each time it meets, coming up
  or past. The folders give the country and the page's address:
  data/meetups/pl/clojure-warsaw/ is /meetups/clojure-warsaw/, in Poland. Its
  photos are in static/photos/pl/clojure-warsaw/. The templates in
  data/templates/ show what goes in the files."
  (:require [app.countries :as countries]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File)
           (java.net URI)
           (java.time LocalDate LocalTime ZoneId)))

(def ^:private formats #{:in-person :hybrid :online})

(def ^:private link-labels
  {"meetup.com"  "Meetup.com"
   "luma.com"    "Luma"
   "lu.ma"       "Luma"
   "heylo.com"   "Heylo"
   "discord.com" "Discord"
   "discord.gg"  "Discord"
   "youtube.com" "YouTube"
   "youtu.be"    "YouTube"
   "github.com"  "GitHub"})

(defn- link
  "A link is its URL, labeled after its site, or a map with its own :label
  and the :url."
  [l]
  (let [{:keys [label url]} (if (string? l) {:url l} l)
        host (some-> (try (.getHost (URI. url)) (catch Exception _ nil))
                     (str/replace #"^www\." ""))]
    {:label (or label (link-labels host (or host url)))
     :url   url}))

(defn- paragraphs
  "A blank line starts a new paragraph. Other line breaks and indentation are
  only spaces, so a long description can be wrapped in the file."
  [s]
  (some->> (str/split (str/trim (str s)) #"\n\s*\n")
           (map #(str/replace (str/trim %) #"\s+" " "))
           (remove str/blank?)
           (seq)
           (str/join "\n\n")))

(defn- fail [file problem]
  (throw (ex-info (str file ": " problem) {:file (str file)})))

(defn- parses? [parse s]
  (try (parse s) true (catch Exception _ false)))

(defn- read-edn
  "An empty file is an empty map, so a date can be announced with nothing
  else."
  [^File file]
  (let [v (try (or (edn/read-string (slurp file)) {})
               (catch Exception e (fail file (str "isn't valid EDN, " (ex-message e)))))]
    (if (map? v) v (fail file "should hold a map, in {}"))))

(defn- link-problem [links]
  (some #(when-not (re-matches #"https?://\S+" (str %))
           (str (pr-str %) " should be a full link, starting with https://"))
        (map (comp :url link) links)))

(defn- meetup-problem [{:keys [name format links]}]
  (cond
    (str/blank? name)      "it has no :name"
    (not (formats format)) ":format should be :in-person, :hybrid or :online"
    :else                  (link-problem links)))

(defn- event-problem
  "`photo-dir` is where the meetup's photos are, like /photos/pl/clojure-warsaw/."
  [photo-dir {:keys [time time-zone url links photos]}]
  (cond
    (and time (not (parses? #(LocalTime/parse %) time)))
    (str ":time should be like \"19:00\", not " (pr-str time))

    (and time-zone (not (parses? #(ZoneId/of %) time-zone)))
    (str ":time-zone should be like \"Europe/Warsaw\", not " (pr-str time-zone))

    :else
    (or (some #(when-not (str/starts-with? (str %) photo-dir)
                 (str (pr-str %) " should start with " photo-dir ", for a photo in static" photo-dir))
              photos)
        (link-problem (cond-> (vec links) url (conj url))))))

(defn- read-event [photo-dir ^File file]
  (let [date (str/replace (.getName file) #"\.edn$" "")]
    (when-not (and (re-matches #"\d{4}-\d{2}-\d{2}" date) (parses? #(LocalDate/parse %) date))
      (fail file "should be meetup.edn, or named after a date, like 2027-01-12.edn"))
    (let [event (read-edn file)]
      (some->> (event-problem photo-dir event) (fail file))
      (-> event
          (assoc :date date)
          (update :links #(mapv link %))))))

(defn- entries
  "What's in `dir`, without hidden files like .DS_Store."
  [dir]
  (->> (.listFiles (io/file dir))
       (remove #(str/starts-with? (.getName ^File %) "."))
       (sort)))

(defn- read-meetup [code country ^File dir]
  (let [slug      (.getName dir)
        info      (io/file dir "meetup.edn")
        photo-dir (str "/photos/" code "/" slug "/")]
    (cond
      (not (.isDirectory dir))
      (fail dir "should be a folder per meetup, like data/meetups/pl/clojure-warsaw/")

      (not (re-matches #"[a-z0-9]+(-[a-z0-9]+)*" slug))
      (fail dir "the folder's name should be lower-case words with dashes, like clojure-warsaw")

      (not (.isFile info))
      (fail dir "has no meetup.edn"))
    (when-let [other (first (remove #(str/ends-with? (.getName ^File %) ".edn") (entries dir)))]
      (fail other (str "only .edn files go here, photos go in static" photo-dir)))
    (let [meetup (read-edn info)]
      (some->> (meetup-problem meetup) (fail info))
      (-> meetup
          (assoc :slug    slug
                 :country country
                 :events  (->> (entries dir)
                               (remove #(= "meetup.edn" (.getName ^File %)))
                               (map #(read-event photo-dir %))
                               (sort-by :date)
                               (vec)))
          (update :description paragraphs)
          (update :links #(mapv link %))))))

(defn- read-country [^File dir]
  (let [country (countries/by-code (.getName dir))]
    (when-not (and country (.isDirectory dir))
      (fail dir "should be a folder named after a country's two-letter code in lower case, like br"))
    (map #(read-meetup (.getName dir) country %) (entries dir))))

(defn- unique-slugs [meetups]
  (doseq [[slug n] (frequencies (map :slug meetups))
          :when (> n 1)]
    (throw (ex-info (str n " meetup folders are called " slug ", and their pages would all be /meetups/"
                         slug "/. Rename all but one.")
                    {:slug slug})))
  meetups)

(defn read-dir
  "The meetups in `dir`, by country code, then folder."
  [dir]
  (->> (entries dir)
       (mapcat read-country)
       (vec)
       (unique-slugs)))
