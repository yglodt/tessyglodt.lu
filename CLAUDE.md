# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.1 web application (Java 21 target — production needs a Java 21+ runtime; runs fine on JDK 25) for tessyglodt.lu — a geographic CMS about Luxembourg municipalities, cantons, and districts ("Kierchtuerms­promenaden"). Uses PostgreSQL and server-side rendering with Thymeleaf.

## Build and Development Commands

```bash
mvn clean package                                   # Build JAR (target/tessyglodt.jar), minify CSS, strip HTML whitespace
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Run locally on port 8080 against the local DB
mvn test                                            # Run all tests (there are none yet)
```

- **Always run locally with the `dev` profile** (`application-dev.properties`: Postgres on port 5433, DEBUG logging). Without it the app connects to port 5432 and fails with `role "tessyglodt" does not exist`. VS Code: use the "Application (dev)" launch config in `.vscode/launch.json`.
- Local admin login: `admin` / `password`.
- **javac crash workaround:** when the code has compile errors, in-process javac on JDK 25 may crash with `Cannot load from object array because "this.hashes" is null` instead of reporting them. Run `mvn compile -Dmaven.compiler.fork=true` to see the real errors.
- The local dev app normally runs from VS Code on **port 8080**. Devtools reloads it on changes, including template and static-file edits, so check changes there first.
- Don't `mvn clean` underneath the running app (devtools restarts on half-built classes). To test a full packaged build, run a separate instance: `java -jar target/tessyglodt.jar --spring.profiles.active=dev --server.port=8081`.
- `localhost` serves the unminified `style.css`; open the site via `127.0.0.1` to get `s.min.css` as in production.

## Configuration

- `src/main/resources/application.properties` holds defaults (committed, including the placeholder admin password — intentional).
- **Production** runs `/applications/tessyglodt.jar` as user `apps` (see `tessyglodt.lu.service`) with `--spring.config.location=/etc/tessyglodt.properties`. That option **replaces** the bundled `application.properties`, so none of its defaults apply in production: every key production needs must be in `/etc/tessyglodt.properties`, and `@Value` placeholders need inline defaults (`${key:default}`) or a missing key stops the app from starting (this happened with `facebook.enabled`).
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
- Full-text search: `to_tsvector(unaccent(content)) @@ websearch_to_tsquery(unaccent(?))` — `websearch_to_tsquery` so arbitrary user input (multiple words, `&`, `(`) doesn't raise tsquery syntax errors.
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

- `/sich` — search
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

- **Thymeleaf** with layout dialect. Main layout: `templates/layouts/layout.html`; pages use `layout:decorate="~{layouts/layout}"` and their own `<head>` (merged by the layout dialect, title via `layout:title-pattern`). Fragments are included with `th:replace="~{fragments/...}"`.
- **CKEditor** for admin rich text editing; the edit form's textarea uses `th:text` (escaped), page display uses `th:utext`.
- Fonts (Italianno, Smythe, Lora, Material Icons) are self-hosted in `static/fonts/`, with their `@font-face` rules at the top of `style.css` (Latin + Latin-Extended subsets only). No Google Fonts, no analytics.
- **Maps** (`map.html`, `page.html`) use Leaflet 1.9.4 from cdnjs (with SRI hashes; `<link>`/`<script>` sit in each page's own `<head>`) and OpenStreetMap tiles, with no API key. Google Maps was dropped in Sept 2026 after its key stopped working. OSM's tile policy requires the attribution and allows only light use; switch to a tile provider if traffic grows a lot.
- **Thymeleaf inline JS gotcha:** write `[ [[${x}]]` (with a space), never `[/*[[${x}]]*/` or `[[[${x}]]`, because `[/` parses as a closing element and breaks rendering mid-response (`ERR_INCOMPLETE_CHUNKED_ENCODING`).
- External requests from pages: cdnjs (Leaflet), `tile.openstreetmap.org`, and the Facebook SDK (only when not on localhost).
- Random page teaser: `Page.getTeaser()` (plain text via Jsoup, 450 chars).
- Build step: Maven `replacer` plugin strips whitespace between tags in templates; `minify` plugin compresses `style.css` → `s.min.css` (used when not on localhost). Static URLs are content-hashed (`spring.web.resources.chain.strategy.content`, e.g. `s.min-<hash>.css`), so long browser caching is safe.
- Templates validate with the W3C Nu checker except for the known items below. To check: render pages from a running instance and run `vnu.jar` (npm package `vnu-jar`).

### Facebook posting

`FacebookService` posts a random published page (title, 📍 municipality and canton, the teaser, and "Weiderliesen op tessyglodt.lu: <url>" since Facebook can't put links behind text; the link card comes from the `link` parameter) to https://www.facebook.com/Kierchtuermspromenaden via Graph API `POST /{page_id}/feed`, daily at `facebook.cron` (default 7:15 Europe/Luxembourg). Off unless `facebook.enabled=true`; `facebook.page-id` and `facebook.access-token` (a long-lived Page token from the Meta app "Kierchtuermspromenaden", Live mode, with `pages_manage_posts` + `pages_read_engagement`; posts from a Development-mode app aren't public) live only in `/etc/tessyglodt.properties`. `GET /admin/facebook/post-now` posts one immediately and returns the result as plain text, which also checks the token. `page.html` has Open Graph tags (no `og:image`: pages have no images of their own) for the link preview. Location tagging was left out: `place` needs a Facebook Place page ID and place search is deprecated.

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

Systemd service file: `tessyglodt.lu.service`, a copy of `/etc/systemd/system/tessyglodt.lu.service` on the server.

## Known Technical Debt / Open Items

- **No tests.** A few integration tests (home, page, search, 404, hidden page, admin login) would catch most regressions.
- **HTML:** sidebar headings jump from `<h2>` to `<h4>` (kept deliberately — changing affects styling); header text `d&nbsp;'Lëtzebuerger` renders with a space before the apostrophe; Thymeleaf's auto-generated CSRF input ends in `/>` (harmless).
- **Facebook Like button** (`layout.html`) loads the legacy `connect.facebook.net/en_US/all.js` SDK, which probably no longer works and sends visitor data to Facebook. Candidate for removal (a plain link to the Facebook page would do).
- **Social posting:** Facebook posting exists (see above). The Twitter integration was removed (X API is pay-per-use since Feb 2026: ~$0.20 per post with a link; twitter4j used the retired v1.1 endpoint). If re-added, use X API v2 (`POST /2/tweets`) or Bluesky/Mastodon (free).
- **Search performance:** `to_tsvector` is computed per query over all pages; fine at ~560 pages, add a stored tsvector column + GIN index if it grows.

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

## Local Database Setup (Postgres.app on macOS)

The local server for this project listens on **port 5433** (a different Postgres instance runs on 5432).

Restoring a `pg_dump` backup:
```bash
# Add Postgres.app CLI tools to PATH (add to ~/.zshrc for permanence)
export PATH="/Applications/Postgres.app/Contents/Versions/latest/bin:$PATH"
export PGPORT=5433

# Create role and database (the dump's objects are owned by "tessyglodt")
psql -d postgres -c "CREATE ROLE tessyglodt WITH LOGIN PASSWORD 'tessyglodt';"
createdb -O tessyglodt tessyglodt

# Restore custom format (-Fc, .dump)
pg_restore --no-privileges -d tessyglodt /path/to/backup.dump

# Restore plain SQL format (.sql)
psql -d tessyglodt < /path/to/backup.sql

# If the backup was made by a different user
pg_restore --no-owner --no-privileges -d tessyglodt /path/to/backup.dump
```

Don't keep backup dumps in the project folder unignored — `*.dump` is not in `.gitignore`.
