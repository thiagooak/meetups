# Clojure Meetups

Map and directory of Clojure meetups, with pages for organizers on adding
their meetup, getting it sponsored by the Clojure Meetup Pilot Program, and
keeping it up to date. A static site.

It started as [Magda's Ring app](https://github.com/maguse05/clojure-first-steps),
page for page, but built ahead of time from EDN files instead of the
applications spreadsheet and its Apps Script. Pages are rendered with
[Hiccup](https://github.com/weavejester/hiccup), the map uses
[Leaflet](https://leafletjs.com) and OpenStreetMap tiles.

## Building it

```sh
clojure -X:build               # writes the site to dist/
clojure -X:serve               # builds, then serves it on http://localhost:8080
clojure -X:serve :port 3000
clojure -X:test
```

`serve` doesn't watch for changes: run `clojure -X:build` again, or
`(app.site/build nil)` from a REPL, and reload.

## Meetups

Every meetup is a folder in its country's, named with the two-letter code:

```
data/meetups/pl/clojure-warsaw/meetup.edn            what the meetup is
data/meetups/pl/clojure-warsaw/2026-11-03.edn        a time it met
data/meetups/pl/clojure-warsaw/2027-01-12.edn        the next time it meets
static/photos/pl/clojure-warsaw/2026-11-03-1.jpg     photos, named after the date
```

The folders give the country, Poland, and the page's address,
`/meetups/clojure-warsaw/`. Keep the name once it's published, links to the
page depend on it, and it has to be unique across countries.

A dated file is the meetup's next date until that day passes; then it's one
of its past meetups, with whatever photos and links it has by then. Without
a dated file to come, the next date is TBA. An empty file is enough to
announce a date.

In `meetup.edn`, only `:name` and `:format` are required. Without a
description the page has no About section. A link is a URL, labeled after
its site, or `{:label "Slides" :url "https://..."}`. A dated file's
`:photos` are paths like `/photos/pl/clojure-warsaw/2026-11-03-1.jpg`, and
have to be in the meetup's own photo folder.

Organizers add and update their meetups with pull requests, and the site's
pages walk them through it:

| Page | |
| --- | --- |
| `/add-your-meetup/` | Shows `data/templates/meetup.edn`, a commented `meetup.edn`. |
| `/update-your-meetup/` | Shows `data/templates/event.edn`, a commented dated file. |
| `/sponsorship/` | The pilot program, and a link to its sign-up form. |

`.github/pull_request_template.md` is the checklist those pull requests get.
Change the templates and the pages change with them; `clojure -X:test` checks
that they still make a valid meetup.

The build stops and names the file when something's off, like a country
folder that isn't a code, a file that isn't named after a date, a `:time`
that isn't like `"19:00"` or a link that doesn't start with `https://`.
`clojure -X:test` also checks that every photo is in `static/` and every
meetup is on the map.

## Settings

`data/site.edn` has:

- `:repo-url` and `:repo-branch`: where the organizer pages send people to
  edit files and open pull requests.
- `:sponsorship-form-url`: the pilot program's sign-up form.
- `:newsletter-form`: a Mailchimp embedded form (Audience → Signup forms →
  Embedded forms has the URL), which gets the address as `EMAIL`. `:fields`
  adds hidden ones, like the id of a Mailchimp tag as `tags`. If Mailchimp
  sends people back to `/?status=subscribed#newsletter`, the page thanks them.

The build warns while the sign-up form or the newsletter has no URL.

## Layout

| Path | |
| --- | --- |
| `data/meetups/` | A file per meetup. |
| `data/templates/` | The templates the organizer pages show. |
| `data/site.edn` | Repository, sign-up form and newsletter settings. |
| `static/` | Copied to the site as it is: styles, scripts, images, photos. |
| `src/app/site.clj` | The build: reads `data/`, writes `dist/`. |
| `src/app/meetups.clj` | Reads and checks the meetup files. |
| `src/app/countries.clj` | Country names for the filter. |
| `src/app/views/` | Hiccup views: layout, home, meetup page, map, organizer pages. |
| `static/js/` | Map interactions, country filter, the newsletter's thank-you note. |
| `static/style.css` | Styles, following clojure.org's palette. |

## Deploying

`dist/` works on any static host (GitHub Pages, Netlify, Vercel, Cloudflare
Pages, S3): pages are `index.html` files in folders, and the host's 404 page
is `404.html`. The site expects to be at the root of its domain.

Which events are coming up, and the order of the meetup list, are worked out
when the site is built, so rebuild it at least once a day, like with a
scheduled CI job, or a past event keeps showing as next.

## Security

- No secrets: the site has no server and no API keys. Mailchimp's embedded
  form URL is public by design.
- Changes to meetups arrive as pull requests, so nothing reaches the site
  without a maintainer merging it. The checklist asks contributors to be the
  meetup's organizers, or have their agreement, and to have the consent of
  the people in the photos.
- Everything from the data files is escaped by Hiccup. Links must be
  `http(s)://`, so a file can't add a `javascript:` link, and file names must
  be lower-case words, so a meetup can't write outside its folder in `dist/`.
  The map's JSON escapes `/`, so it can't close its `<script>` tag.
- Leaflet comes from unpkg with Subresource Integrity hashes, so a tampered
  copy won't run.
- `serve` only listens on localhost.
- The meetup files are public. Only put in them what organizers agreed to show.
