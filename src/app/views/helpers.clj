(ns app.views.helpers
  (:require [clojure.string :as str])
  (:import (java.time LocalDate LocalTime ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Locale)))

(def ^:dynamic *base-path*
  "Where the site is served from: \"\" at the root of a domain, \"/<repo>\" on
  GitHub Pages without a custom domain."
  "")

(defn url
  "A path on this site, like \"/style.css\", under the base path."
  [path]
  (str *base-path* path))

(def format-labels
  (array-map :in-person "In person"
             :hybrid    "Hybrid"
             :online    "Online"))

(defn format-badge [meetup-format]
  [:span {:class (str "badge badge--" (name meetup-format))}
   (format-labels meetup-format)])

(defn meetup-path [{:keys [slug]}]
  (url (str "/meetups/" slug "/")))

(defn place [{:keys [city country]}]
  (str/join ", " (remove str/blank? [city country])))

(defn- starts-at ^ZonedDateTime [{:keys [date time time-zone]}]
  (ZonedDateTime/of (LocalDate/parse date)
                    (if time (LocalTime/parse time) LocalTime/MAX)
                    (ZoneId/of (or time-zone "UTC"))))

(defn- upcoming? [event]
  (let [start (starts-at event)]
    (.isAfter start (ZonedDateTime/now (.getZone start)))))

(defn upcoming-event
  "The meetup's soonest event that hadn't happened yet when the site was
  built. Events without a `:time` count as upcoming until the end of their
  day."
  [{:keys [events]}]
  (some #(when (upcoming? %) (assoc % :starts-at (starts-at %))) events))

(defn past-events
  "The events that had happened when the site was built, latest first."
  [{:keys [events]}]
  (reverse (remove upcoming? events)))

(defn by-next-event
  "Meetups with an upcoming event first, soonest first; the rest by name."
  [meetups]
  (let [start (fn [m] (some-> (upcoming-event m) ^ZonedDateTime (:starts-at) (.toInstant)))]
    (sort-by (juxt (comp nil? start) start :name) meetups)))

(def ^:private date-formats
  {:short (DateTimeFormatter/ofPattern "EEE d MMM yyyy" Locale/ENGLISH)
   :long  (DateTimeFormatter/ofPattern "EEEE, d MMMM yyyy" Locale/ENGLISH)})

(def ^:private time-format
  (DateTimeFormatter/ofPattern "HH:mm z" Locale/ENGLISH))

(def ^:private day-format
  (DateTimeFormatter/ofPattern "d MMMM yyyy" Locale/ENGLISH))

(defn day
  "A plain `yyyy-MM-dd` date, as \"3 November 2026\"."
  [date]
  (.format (LocalDate/parse date) day-format))

(defn youtube-id [url]
  (second (re-find #"(?:youtube\.com/(?:watch\?(?:.*&)?v=|live/|embed/|shorts/)|youtu\.be/)([\w-]{11})" (str url))))

(defn event-date [{:keys [^ZonedDateTime starts-at time]} style]
  (cond-> (.format starts-at ^DateTimeFormatter (date-formats style))
    (and time (= :long style)) (str " · " (.format starts-at time-format))))
