(ns app.countries
  (:require [clojure.string :as str])
  (:import (java.text Collator)
           (java.util Locale Locale$Builder)))

(defn- display-name [iso-code]
  (-> (Locale$Builder.)
      (.setRegion iso-code)
      (.build)
      (.getDisplayCountry Locale/ENGLISH)))

(defn sorted
  "Country names in alphabetical order, the accented ones included."
  [names]
  (sort (Collator/getInstance Locale/ENGLISH) names))

(def names
  (vec (sorted (map display-name (Locale/getISOCountries)))))

(def aliases
  {"usa"                      "United States"
   "us"                       "United States"
   "united states of america" "United States"
   "uk"                       "United Kingdom"
   "england"                  "United Kingdom"
   "great britain"            "United Kingdom"
   "czech republic"           "Czechia"
   "brasil"                   "Brazil"})

(def ^:private codes (set (Locale/getISOCountries)))

(defn by-code
  "The country for a lower-case two-letter ISO code, like `br`."
  [code]
  (when (and (re-matches #"[a-z]{2}" (str code)) (codes (str/upper-case code)))
    (display-name (str/upper-case code))))
