# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.1 web application (Java 25, in production and for development) for tessyglodt.lu — a geographic CMS about Luxembourg municipalities, cantons, and districts ("Kierchtuerms­promenaden"). Uses PostgreSQL and server-side rendering with Thymeleaf.

## Build and Development Commands

```bash
./dev.sh                                            # Local dev in Docker: Postgres 18 + the app with hot reload on port 8017
mvn clean package                                   # Build JAR (target/tessyglodt.jar), minify CSS, strip HTML whitespace
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Run on the host instead, against the Docker DB
mvn test                                            # Run all tests (there are none yet)
```

- **Always run locally with the `dev` profile** (`application-dev.properties`: Postgres on port 5417, app on port 8017, DEBUG logging). Without it the app connects to port 5432 and fails with `role "tessyglodt" does not exist`. VS Code: use the "Application (dev)" launch config in `.vscode/launch.json`.
- Local admin login: `admin` / `password`.
- **javac crash workaround:** when the code has compile errors, in-process javac on JDK 25 may crash with `Cannot load from object array because "this.hashes" is null` instead of reporting them. Run `mvn compile -Dmaven.compiler.fork=true` to see the real errors.
- **Local dev runs in Docker** (`compose.yaml`, dev only; production has its own setup): `./dev.sh` starts the `db` and `app` containers, `./dev.sh db` (only the DB, for `tools/` and the app run from VS Code), `./dev.sh psql`, `./dev.sh import FILE`, `./dev.sh stop`. The `app` container (`maven:3-eclipse-temurin-25`, source mounted at `/workspace`) runs `./dev.sh container`: `mvn spring-boot:run` with devtools, plus a loop that recompiles when a `.java` file or the pom changes, so devtools restarts the app. Templates and static files are read straight from `src/` (`SPRING_THYMELEAF_PREFIX`, `SPRING_WEB_RESOURCES_STATIC_LOCATIONS` in `compose.yaml`), so they show without a rebuild. The container's `target/` and `~/.m2` are Docker volumes, so it doesn't share build output with the host. The app listens for a debugger on port 5017: VS Code launch config "Attach to app in Docker" (`.vscode/launch.json`), from the host or from inside the container. `.devcontainer/devcontainer.json` ("Dev Containers: Reopen in Container") opens VS Code inside the same container; `tessyglodt.lu.code-workspace` is the workspace file, with the recommended extensions. Check changes on **port 8017** (http://localhost:8017) first. The app can also run from VS Code on the host ("Application (dev)"), against the Docker DB on 5417; stop the `app` container first, both use port 8017.
- Don't `mvn clean` underneath the running app (devtools restarts on half-built classes). To test a full packaged build, run a separate instance: `java -jar target/tessyglodt.jar --spring.profiles.active=dev --server.port=8081`.
- `localhost` serves the unminified `style.css`; open the site via `127.0.0.1` to get `s.min.css` as in production.

## Configuration

- `src/main/resources/application.properties` holds defaults (committed, including the placeholder admin password — intentional).
- **Production** starts the JAR with `--spring.config.location` pointing to its own properties file on the server. That option **replaces** the bundled `application.properties`, so none of its defaults apply in production: every key production needs must be in that file, and `@Value` placeholders need inline defaults (`${key:default}`) or a missing key stops the app from starting (this happened with `facebook.enabled`).
- **Local secrets** (e.g. the Facebook token for testing) go in `config/application-dev.properties` in the project root, which is git-ignored and loaded by Boot on top of the classpath file. Never put tokens in `src/main/resources/*.properties`: they're tracked and the repo is public.
- `spring-boot-properties-migrator` is still in the pom to report renamed keys in the production config after the Boot 4 upgrade; remove it once the production startup log shows no migration warnings.

## Architecture

### Package Structure (`lu.tessyglodt.site`)

- `controller/` — `WebController` (public pages), `AdminController` (`/admin/**`, requires ADMIN role), `NotFoundAdvice` (maps `EmptyResultDataAccessException` to 404)
- `service/` — Business logic with `@Cacheable`/`@CacheEvict`. `PageService` is the main service; also `CantonService`, `DistrictService`, `MunicipalityService`, and `FacebookService` (see Facebook posting).
- `data/` — Domain objects (Page, Municipality, Canton, District) and their `RowMapper` implementations
- `spring/` — `ConfigWebMvc` (caching, interceptor), `ConfigWebSecurity` (two `SecurityFilterChain`s)
- `MyHandlerInterceptor` — adds `now`, `req` (the layout needs it, including on error pages) and, for admins, `hiddenPages` to every non-redirect model

### Conventions

- **Constructor injection** with `private final` fields — no field `@Autowired`.
- `JdbcTemplate` calls use varargs (`query(sql, mapper, args...)`), not `new Object[] {...}`.
- Code style: tabs, tab-aligned field declarations, `final` parameters.
- **Line endings are mixed:** most Java files, `pom.xml` and `application.properties` use CRLF; some templates use LF. Preserve each file's existing line endings when editing (a rewrite to LF shows up as a whole-file diff).

### Database

- **Direct JDBC** via Spring's `JdbcTemplate` — no JPA/Hibernate. Connection pool: HikariCP (via `spring-boot-starter-jdbc`).
- **PostgreSQL** with a custom `slugify()` database function used for URL matching of cantons/districts.
- Full-text search (`PageService.search()`): stored generated column `page.search` (title weight A, text without tags weight B) with a GIN index, built with the text search config `public.english_unaccent` (english with `unaccent` before stemming); set up in the database (local and production) by hand; the definition is in any `pg_dump`. Postgres recomputes the column on every insert/update, so the app never writes it. Query: `search @@ websearch_to_tsquery('public.english_unaccent', ?)` — `websearch_to_tsquery` so arbitrary user input (multiple words, `&`, `(`, `"phrase"`) doesn't raise tsquery syntax errors. Ordered by `ts_rank`, 20 per page (`/sich?q=…&p=2`), excerpts from `ts_headline` for the shown page only. `ts_headline` marks hits with the control characters `\u0002`/`\u0003`, so Java can unescape entities, HTML-escape the excerpt and only then turn the markers into `<mark>`. No prefix matching (`kierch:*`) for now: compound words like *Kierchtuerm* aren't found by `Kierch`.
- The `orders` table exists in the DB but is no longer used by the code.
- No schema files in the repo; a local `pg_dump` is the reference.

### Published / hidden pages

`page.published` ("Siichtbar" in the admin form) hides a page:
- All public lists, the map, search, feeds, sitemap, random page and `/stats` filter `where published` (`PageService.getPagesWithWhere()` always adds it).
- `/page/{name}` returns 404 for hidden pages unless the user is ADMIN.
- Admins see hidden pages listed as "Verstoppt:" in the admin bar (`PageService.getUnpublishedPages()`), and in `/stats`.
- The page form sends a hidden `_published` marker so an unchecked checkbox binds to `false` (the `Page` default is `true`).

### Caching

Two cache regions in `ConcurrentMapCacheManager`:
- `page` — page content and listings (incl. hidden pages list)
- `accessInfo` — view-count-derived lists (last read, most read, newest)

Mutations (`insert`, `update`) evict both caches. `getPageByProperty` evicts `accessInfo`, so in `WebController.getPage()` the view count is updated **before** `getPageByProperty()` — otherwise a concurrent request could re-cache stale lists. `updateViewCount` only touches published pages.

### URL Routes (Luxembourgish)

- `/sich` — search, with excerpts and paging (`p`)
- `/kaart` — map
- `/apropos` — about
- `/auteur` — author
- `/canton/{slug}`, `/district/{slug}` — geographic browsing (slug = `slugify(name)`, e.g. `/canton/wolz`)
- `/page/{name}` — individual page (updates view count, excludes bots)
- `/stats` — view counts
- `/dateschutz` — privacy page (`privacy.html`), deliberately **not linked** anywhere on the site and not in the sitemap; it only exists as the privacy policy URL in the Meta app settings
- `/feed/nei.xml`, `/feed/alles.xml` — Atom feeds
- `/robots.txt` (blocks `/admin/`, `/login`, `/sich`) and `/sitemap.xml` (static pages, cantons, districts, published pages with `lastmod`; built in `WebController`, entries cached in `page`)
- Unknown page/canton/district → 404 page (`templates/error/404.html`)

### View Layer

- **Thymeleaf** with layout dialect. Main layout: `templates/layouts/layout.html`; pages use `layout:decorate="~{layouts/layout}"` and their own `<head>` (merged by the layout dialect, title via `layout:title-pattern`). Fragments are included with `th:replace="~{fragments/...}"`. Each page's content element is `<main layout:fragment="content">`: the page's element *replaces* the layout's placeholder, so a `th:block` there would leave no `<main>` (admin templates still do this).
- **CKEditor** for admin rich text editing; the edit form's textarea uses `th:text` (escaped), page display uses `th:utext`. `pageform.html` sets `CKEDITOR_BASEPATH` because the content-hashed `ckeditor-<hash>.js` name stops CKEditor from finding its own folder. The page form also loads Bootstrap, which overrides some site styles there (admin only).
- **Design:** "book" look — one paper sheet on a desk-coloured background, colours as CSS variables in `:root` of `style.css` (`--accent` dark red for drop caps, ornaments, hover), text column limited to `--measure` (36em) with the sidebar as margin notes (`.with-margin`). Text pages: small-caps kicker (canton · municipality), title, ornament (`<hr class="ornament">`, an SVG data URI), drop cap on the first paragraph only, previous/next page in alphabetical order (`PageService.getPageNeighbours(name)`: a name → `PageNeighbours` map built by `getPagesInfo()` whenever it loads the list, cleared in `insert`/`update`), shown at the bottom and as a `.running-head` at the top, which becomes fixed tabs at the window edges from 1300px. Following one turns the page with a CSS-only cross-document view transition: the old `.sheet` only gets a `view-transition-name` while an `a[rel=next]`/`a[rel=prev]` is hovered or focused, so other navigations stay a plain swap (Firefox has no cross-document transitions and just loads the page). Home: random page as a featured text, short lists in a band (`.shelf`), all texts as an A–Z index (`Utils.groupByInitial()`, accents ignored). No Tailwind/Bootstrap on public pages; plain CSS. Phone breakpoint 600px, tablet 900px; there is a print stylesheet.
- Fonts (Italianno, Smythe, Lora regular/italic/bold, Material Icons — the latter only used in admin) are self-hosted in `static/fonts/`, with their `@font-face` rules at the top of `style.css` (Latin + Latin-Extended subsets only). No Google Fonts, no analytics.
- **Maps** (`map.html`, `page.html`) use Leaflet 1.9.4 from cdnjs (with SRI hashes; `<link>`/`<script>` sit in each page's own `<head>`) and OpenStreetMap tiles, with no API key. Google Maps was dropped in Sept 2026 after its key stopped working. OSM's tile policy requires the attribution and allows only light use; switch to a tile provider if traffic grows a lot.
- **Thymeleaf inline JS gotcha:** write `[ [[${x}]]` (with a space), never `[/*[[${x}]]*/` or `[[[${x}]]`, because `[/` parses as a closing element and breaks rendering mid-response (`ERR_INCOMPLETE_CHUNKED_ENCODING`).
- External requests from pages: cdnjs (Leaflet) and `tile.openstreetmap.org`. The footer links to the Facebook page (no SDK).
- Random page teaser: `Page.getTeaser()` (plain text via Jsoup, 450 chars).
- "An der Géigend" box on `page.html`: the 5 nearest published pages with straight-line distance (`PageService.getNearbyPages()`, flat-earth formula in SQL). The map itself stays on the whole-country view.
- Build step: Maven `replacer` plugin strips whitespace between tags in templates; `minify` plugin compresses `style.css` → `s.min.css` (used when not on localhost). Static URLs are content-hashed (`spring.web.resources.chain.strategy.content`, e.g. `s.min-<hash>.css`), so long browser caching is safe.
- **YUI CSS minifier gotchas:** it strips spaces inside custom property values (write `%20` in data URIs) and around `+`/`-`, so don't use `calc()`/`clamp()` arithmetic like `1rem + 2vw`. After CSS changes, check `s.min.css` in a packaged build.
- Templates validate with the W3C Nu checker except for the known items below. To check: render pages from a running instance and run `vnu.jar` (npm package `vnu-jar`).

### Facebook posting

`FacebookService` posts a published page (title · 📍 municipality and canton on one line, the teaser, and the bare page URL on the last line since Facebook can't put links behind text; the link card comes from the `link` parameter) to https://www.facebook.com/Kierchtuermspromenaden via Graph API `POST /{page_id}/feed`, daily at `facebook.cron` (default 7:15 Europe/Luxembourg). Off unless `facebook.enabled=true`; `facebook.page-id` and `facebook.access-token` (a long-lived Page token from the Meta app "Kierchtuermspromenaden", Live mode, with `pages_manage_posts` + `pages_read_engagement`; posts from a Development-mode app aren't public) live only in the production properties file. Each post is logged in the `facebook_post` table (`page_id` → `page.id`, `post_id`, `date_posted`); the next page is a random one among those never posted, else among those posted longest ago, so all published pages come up once before any repeats. `GET /admin/facebook/post-now` posts one immediately and returns the result as plain text, which also checks the token. `page.html` has Open Graph tags (no `og:image`: pages have no images of their own) for the link preview. Location tagging was left out: `place` needs a Facebook Place page ID and place search is deprecated.

### Security

- `ConfigWebSecurity` (Spring Security 7 lambda DSL):
  - Chain 1 (`@Order(1)`): static paths (`/b/**`, `/ckeditor/**`, `/css/**`, `/fonts/**`, `/img/**`, `/js/**`) — permitAll, no session, **cache-control headers disabled** so browsers keep caching static files.
  - Chain 2: `/admin/**` requires ADMIN, everything else permitAll; form login at `/login` posting to `/authcheck`; GET `/logout` allowed (the layout uses a plain link).
- Admin credentials come from `spring.security.user.*` (overridden in production).

### Key Patterns

- `AdminController.@InitBinder` registers custom `PropertyEditorSupport` instances to bind form select values (integers) to domain objects (Municipality, Canton, District).
- `PageService.getPageByProperty(property, value, log)` builds SQL with the `property` parameter concatenated into the query — callers only pass hardcoded strings ("id", "name").
- Bot detection in `WebController.getPage()` checks the user-agent for known crawler strings before incrementing the view count.

### Geographic Hierarchy

District → Canton → Municipality → Page. The `PageMapper` reconstructs this nested relationship from joined query result columns (`dist_*`, `can_*`, `mun_*`).

### Deployment

Runs in Docker next to its database. The server builds the app image itself from this repo's `master` branch, so deploy = push to `master`, then rebuild the app container on the server. Anything not pushed isn't deployed. The build uses Maven 3.9 while local builds may use an older Maven, so plugins must not rely on dependencies older Maven versions provided implicitly (hence `plexus-utils` on the minify plugin).

## Known Technical Debt / Open Items

- **No tests.** A few integration tests (home, page, search, 404, hidden page, admin login) would catch most regressions.
- **HTML:** Thymeleaf's auto-generated CSRF input ends in `/>` (harmless).
- **Social posting:** Facebook posting exists (see above). The Twitter integration was removed (X API is pay-per-use since Feb 2026: ~$0.20 per post with a link; twitter4j used the retired v1.1 endpoint). If re-added, use X API v2 (`POST /2/tweets`) or Bluesky/Mastodon (free).
- **Punctuation clean-up of the page texts (done, 28 Oct 2026):** a one-off normalisation of apostrophes, quotation marks („…“), dashes, dots and spaces in `page.content`, which also wrote entities as characters (`&eacute;` → `é`) and removed Word/LibreOffice leftovers and dead `%(link0)` links; applied locally and in production. `tools/punctuation/fix-punctuation.sh` only writes a before/after report and guarded SQL files to `target/`, so it can be re-run when new texts bring the problems back. Page titles were not touched (still open). Details and decisions: `tools/punctuation/README.md`.

## Migration Assessments

### SQLite Migration (assessed March 2025)

**Verdict: Medium-high effort (~30-50 hours), with a critical feature loss (full-text search).**

PostgreSQL-specific features in use:
- `?::date` casts in insert/update queries — trivial to remove
- `now()` in `updateViewCount()` — replace with `CURRENT_TIMESTAMP`
- `slugify()` custom DB function in `CantonService`, `DistrictService`, `PageService` — must reimplement in Java
- `unaccent()` in search — must do accent stripping in Java
- `to_tsvector`/`websearch_to_tsquery`/`@@` full-text search — **no direct SQLite equivalent**. SQLite FTS5 exists but doesn't handle accents and behaves differently. This is the dealbreaker for a Luxembourgish content site.

Other concerns: SQLite is single-writer (concurrent writes block), BigDecimal precision loss with REAL type (store as TEXT), no schema files exist to port.

**Recommendation: Keep PostgreSQL.** It runs fine locally via Postgres.app, and the full-text search + `unaccent` + `slugify` combo is doing real work.

### Thymeleaf Migration (assessed March 2025)

24 templates using layout dialect, fragment composition, `sec:authorize`, `@InitBinder` form binding, inline JavaScript for the Leaflet maps.

**Best option if migrating: htmx (~2-3 weeks, ~72-103 hours)**
- Controllers return HTML fragments, add `hx-get`/`hx-post`/`hx-target` attributes. Can migrate incrementally, one page at a time.
- Keeps: server-side rendering (SEO), Spring Security integration, CKEditor, form validation, no build toolchain needed. ~14KB gzipped.

**Overkill: Vue/React SPA (~4-5 weeks)**
- Would require building a full REST API, reimplement form binding, handle auth client-side, lose SEO. Solves problems this site doesn't have.

**Lateral move: Different template engine like Pebble/FreeMarker (~2-3 weeks)**
- Same architecture, different syntax. Doesn't simplify anything meaningful.

**Recommendation: Stay with Thymeleaf unless there's a strong reason to change.** If modernizing, htmx is the clear winner — incremental migration, no npm/webpack, keeps server-side rendering.

## Local Database

Postgres 18 (`postgres:18-alpine`, as in production) in the `db` container of `compose.yaml`, data in the Docker volume `db`. Inside Docker it is `db:5432`; on the host it is published on **127.0.0.1:5417** (5432 is taken by kids.cloud's Postgres.app, and 8080/5433 clashed with other local apps, hence 8017/5417 for this project), which `application-dev.properties` and the `tools/` scripts use (user, password and database: `tessyglodt`). `DB_PORT=… ./dev.sh` publishes it on another port.

Loading a `pg_dump` (replaces the local database; owner and privileges from the dump are ignored):
```bash
./dev.sh import /path/to/backup.dump   # custom format (-Fc)
./dev.sh import /path/to/backup.sql    # plain SQL
```

`*.dump` is git-ignored, but keep backups out of the project folder anyway: the repo is public.
