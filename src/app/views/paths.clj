(ns app.views.paths
  "The pages for organizers: adding a meetup, getting it sponsored, and
  keeping it up to date. The home page has a card for each."
  (:require [app.views.helpers :as helpers]
            [app.views.layout :as layout]))

(def cards
  [{:href    "/add-your-meetup/"
    :eyebrow "Already run one?"
    :title   "Add your meetup"
    :text    "Every Clojure meetup belongs on the map. Add yours with a pull request, no sponsorship needed."
    :cta     "How to add it →"}
   {:href    "/sponsorship/"
    :eyebrow "Need support?"
    :title   "Get sponsored"
    :text    "Apply to the Clojure Meetup Pilot Program for monthly support with things like venue, food and promotion."
    :cta     "About the pilot →"}
   {:href    "/update-your-meetup/"
    :eyebrow "Got a date?"
    :title   "Update your meetup"
    :text    "Announce your next meetup, and add the photos, slides and recordings once it's happened."
    :cta     "How to update it →"}])

(defn- github
  "A page of the site's repository on GitHub: \"new\" for a new file in the
  folder at `path`, \"tree\" for the folder."
  [{:keys [repo-url repo-branch]} page path]
  (str repo-url "/" page "/" repo-branch "/" path))

(defn- external [href & content]
  [:a {:href href :target "_blank" :rel "noopener"} content " ↗"])

(defn- code
  "Inline code that can wrap after a slash, but not at the dashes of a date."
  [s]
  [:code (interpose [:wbr] (for [part (re-seq #"[^/]*/|[^/]+" s)] [:span part]))])

(defn- page
  "A hero with the page's heading, then the main column and an aside card."
  [site {:keys [title heading description lead]} main aside]
  (layout/page
   {:site site :title title :description description}
   [:section.hero
    [:div.container
     [:h1 heading]
     [:p.hero__lead lead]]]
   [:section.section
    [:div.container
     [:div.meetup-layout
      (into [:div.path-main] main)
      (into [:aside.card.path-aside] aside)]]]))

(defn add-page
  "`template` is data/templates/meetup.edn."
  [site template]
  (page site
        {:title       "Add your meetup"
         :heading     (list "Add your " [:span.highlight "meetup"])
         :description "Already run a Clojure meetup? Add it to this website with a pull request."
         :lead        "Do you already run a Clojure meetup? Add it to this website by submitting a pull request. Every Clojure meetup is welcome, sponsored or not."}
        [[:section
          [:h2 "How it works"]
          [:ol.steps
           [:li "Make a folder for your meetup in its country's, in " (code "data/meetups/") ", like "
            (code "data/meetups/pl/clojure-warsaw/") " for a meetup in Poland. "
            "The country is its two-letter code in lower case, and your folder's name, "
            "in lower-case words with dashes, is your page's address: "
            (code "/meetups/clojure-warsaw/") "."]
           [:li "Copy the template below into it as " (code "meetup.edn")
            ", and fill it in. The comments say what goes where."]
           [:li "Got a date for your next meetup? Add it too, see "
            [:a {:href (helpers/url "/update-your-meetup/")} "Update your meetup"] "."]
           [:li "Open a pull request, and go through its checklist."]
           [:li "We review it, and once it's merged your meetup is on the map."]]]
         [:section
          [:h2 "The template"]
          [:pre.code [:code template]]]]
        [[:p "You can do it all from your browser. On GitHub, type the whole path as the file's name, like "
          (code "pl/clojure-warsaw/meetup.edn") ": the slashes make the folders. "
          "GitHub makes a copy of the repository for you, and offers to open the pull request once you save."]
         [:a.button {:href (github site "new" "data/meetups") :target "_blank" :rel "noopener"}
          "Add your meetup on GitHub ↗"]
         [:p.muted "With git, it's the same file: "
          (code "data/templates/meetup.edn") " in the repository. "
          (code "clojure -X:build") " checks it."]
         [:p "Looking for support to run it? " [:a {:href (helpers/url "/sponsorship/")} "Get sponsored"] "."]]))

(defn sponsorship-page [{:keys [sponsorship-form-url] :as site}]
  (page site
        {:title       "Get your meetup sponsored"
         :heading     (list "Get your meetup " [:span.highlight "sponsored"])
         :description "Apply to the Clojure Meetup Pilot Program for monthly support for your meetup."
         :lead        "Run a Clojure meetup, or want to start one? Apply to the Clojure Meetup Pilot Program. Selected meetups get monthly support for things like venue, food and promotion, and a pin on this map."}
        [[:section
          [:h2 "About the pilot"]
          [:p "The pilot supports a group of Clojure meetups every month, while we learn what helps local communities grow. "
           "Long-running meetups and brand new ones can apply, in person, hybrid or online."]]
         [:section
          [:h2 "What the support covers"]
          [:p "What your meetup needs to happen: the venue, food and drinks, speaker travel, promotion or subscriptions, "
           "within a monthly budget you propose."]]
         [:section
          [:h2 "What we ask in return"]
          [:p "A short report after each meetup, so we can track how the pilot is going and keep it sustainable. "
           "The support doesn't mean employment, or a long-term financial commitment."]]
         [:section
          [:h2 "Applying"]
          [:p "The form asks about you and your meetup: its format, how often it meets, how many people you expect, "
           "how you'll promote it and keep in touch with members, and your budget, with what each part of it is for. "
           "We review every application and get back to you by email."]]]
        [[:a.button {:href (or sponsorship-form-url "#") :target "_blank" :rel "noopener"}
          "Apply to the pilot program ↗"]
         [:p "Just want your meetup on the map? " [:a {:href (helpers/url "/add-your-meetup/")} "Add your meetup"]
          ", no sponsorship needed."]]))

(defn update-page
  "`template` is data/templates/event.edn."
  [site template]
  (page site
        {:title       "Update your meetup"
         :heading     (list "Update your " [:span.highlight "meetup"])
         :description "Announce your next Clojure meetup, and add its photos, slides and recordings once it's happened."
         :lead        "Announce your next meetup, and once it's happened, add its photos, slides and recordings to your page."}
        [[:section
          [:h2 "Each time you meet"]
          [:ol.steps
           [:li "Add a file named after the date to your meetup's folder, like "
            (code "data/meetups/pl/clojure-warsaw/2027-01-12.edn")
            ", from the template below. Until that day, it's the next date on your page."]
           [:li "After the meetup, add what it left to the same file: photos, and links to slides, posts "
            "and recordings. It moves to your page's past meetups by itself."]
           [:li "Put the photos in the same folders under " (code "static/photos/")
            ", like " (code "static/photos/pl/clojure-warsaw/2027-01-12-1.jpg") ", named after the date. "
            "About 1600px wide is plenty, and check that the people in them are happy to be online."]
           [:li "To change anything else, like your links or your usual venue, edit your meetup's "
            (code "meetup.edn") "."]
           [:li "Open a pull request, and go through its checklist."]]]
         [:section
          [:h2 "The template"]
          [:pre.code [:code template]]]]
        [[:p "Your meetup's folder is in " (external (github site "tree" "data/meetups") "the meetups folder")
          " on GitHub, under its country's code. There, Add file → Create new file adds a date."]
         [:p "Its photos go in the same folders under " (external (github site "tree" "static/photos") "the photos folder")
          ": Add file → Upload files."]
         [:p "Not on the map yet? " [:a {:href (helpers/url "/add-your-meetup/")} "Add your meetup"] "."]]))
