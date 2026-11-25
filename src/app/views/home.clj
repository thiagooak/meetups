(ns app.views.home
  (:require [app.countries :as countries]
            [app.views.helpers :as helpers]
            [app.views.layout :as layout]
            [app.views.map :as meetup-map]
            [app.views.paths :as paths]))

(defn- counted
  "\"1 meetup\", \"12 meetups\"."
  [n one many]
  (str n " " (if (= 1 n) one many)))

(defn- meetup-card [meetup]
  (let [event (helpers/upcoming-event meetup)]
    [:li.meetup-card {:data-slug (:slug meetup)}
     [:a.meetup-card__link {:href (helpers/meetup-path meetup)}
      [:span.meetup-card__top
       [:span.meetup-card__place (helpers/place meetup)]
       (helpers/format-badge (:format meetup))]
      [:span.meetup-card__name (:name meetup)]
      [:span.meetup-card__meta
       [:span "Next: " (if event (helpers/event-date event :short) "TBA")]
       [:span (:frequency meetup)]]]]))

(defn- meetup-list
  "Every meetup. js/meetups.js filters them by the country typed in, or the
  one in ?country=, and fills in the empty states."
  [meetups]
  [:section.section {:id "meetups"}
   [:div.container
    [:div.meetups-header
     [:h2 "All meetups"]
     [:form.country-filter {:action "/" :method "get" :role "search"}
      [:label.visually-hidden {:for "country-filter"} "Filter by country"]
      [:input#country-filter {:type "search" :name "country" :list "countries"
                              :placeholder "Filter by country" :autocomplete "off"}]
      [:button.button.button--small {:type "submit"} "Filter"]]]
    [:p.meetups-count {:aria-live "polite"}
     [:span.meetups-count__shown (count meetups)] " of " (counted (count meetups) "meetup" "meetups")]
    [:ul.meetup-grid
     (map meetup-card (helpers/by-next-event meetups))]
    [:div.empty-state {:data-empty "country" :hidden true}
     [:p.empty-state__title
      "There's no Clojure meetup in " [:span.empty-state__country] " yet."]
     [:p "Be the one who brings your local Clojure community together. "
      "Tell us about the meetup you'd like to start and we'll help you get it off the ground."]
     [:a.button {:href "/sponsorship/"} "Apply with your meetup proposal"]]
    [:div.empty-state {:data-empty "none" :hidden true}
     [:p.empty-state__title
      "No meetups match “" [:span.empty-state__query] "”."]
     [:p "Check the spelling, or pick a country from the list."]]]])

(defn- for-organizers
  "Still #apply, so old links to the application form land here."
  []
  [:section.section.apply {:id "apply"}
   [:div.container
    [:div.apply__intro
     [:h2 "For organizers"]
     [:p.apply__lead
      "Run a Clojure meetup, or want to start one? Put it on the map, "
      "get support to run it, and keep its page up to date."]]
    [:ul.path-grid
     (for [{:keys [href eyebrow title text cta]} paths/cards]
       [:li
        [:a.path-card {:href href}
         [:span.path-card__eyebrow eyebrow]
         [:span.path-card__title title]
         [:span.path-card__text text]
         [:span.path-card__cta cta]]])]]])

(defn page [{:keys [meetups] :as opts}]
  (layout/page
   (assoc opts :leaflet? true :scripts ["/js/meetups.js"])
   [:section.hero
    [:div.container
     [:h1 "Find a " [:span.highlight "Clojure meetup"] " near you"]
     [:p.hero__lead
      (counted (count meetups) "meetup" "meetups") " in "
      (counted (count (distinct (map :country meetups))) "country" "countries")
      " bringing Clojurians together, in person and online."]]]
   [:section.section.map-section
    [:div.container
     [:h2.visually-hidden "Meetup map"]
     (meetup-map/meetup-map meetups)]]
   (meetup-list meetups)
   (for-organizers)
   [:datalist#countries
    (for [c (countries/sorted (distinct (map :country meetups)))]
      [:option {:value c}])]))
