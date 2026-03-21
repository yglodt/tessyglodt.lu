# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 2.7 web application (Java 17) for tessyglodt.lu — a geographic CMS about Luxembourg municipalities, cantons, and districts ("Kierchtuerms­promenaden"). Uses PostgreSQL and server-side rendering with Thymeleaf.

## Build and Development Commands

```bash
mvn clean package        # Build JAR (tessyglodt.jar), minify CSS, strip HTML whitespace, run tests
mvn spring-boot:run      # Run on port 8080
mvn test                 # Run all tests
mvn test -Dtest=ClassName  # Run a single test class
```

Note: There are currently no test classes in the project.

## Architecture

### Package Structure (`lu.tessyglodt.site`)

- `controller/` — Two controllers: `WebController` (public pages) and `AdminController` (`/admin/**`, requires ADMIN role)
- `service/` — Business logic with `@Cacheable`/`@CacheEvict` annotations. `PageService` is the main service.
- `data/` — Domain objects (Page, Municipality, Canton, District, Order) and their `RowMapper` implementations
- `spring/` — Configuration: `ConfigWebMvc` (caching, scheduling, interceptors), `ConfigWebSecurity` (Spring Security), `Scheduler` (daily tweet cron)

### Database

- **Direct JDBC** via Spring's `JdbcTemplate` — no JPA/Hibernate
- **PostgreSQL** with a custom `slugify()` database function used for URL matching of geographic entities
- Full-text search uses `to_tsvector`/`to_tsquery` with `unaccent`
- Legacy H2 search path still exists in code but PostgreSQL is the active driver

### Caching

Two cache regions in `ConcurrentMapCacheManager`:
- `page` — page content and listings
- `accessInfo` — view-count-derived lists (last read, most read, newest)

Mutations (`insert`, `update`) evict both caches. `getPageByProperty` evicts `accessInfo` (because it tracks last-read state).

### URL Routes (Luxembourgish)

Public routes use Luxembourgish names:
- `/sich` — search
- `/kaart` — map
- `/apropos` — about
- `/auteur` — author
- `/canton/{slug}`, `/district/{slug}` — geographic browsing
- `/page/{name}` — individual page (updates view count, excludes bots)
- `/feed/nei.xml`, `/feed/alles.xml` — Atom feeds

### View Layer

- **Thymeleaf** with layout dialect. Main layout: `templates/layouts/layout.html`
- `MyHandlerInterceptor` injects `now` (current timestamp) into every model
- **CKEditor** for admin rich text editing
- Build step: Maven `replacer` plugin strips whitespace from HTML templates; `minify` plugin compresses `style.css` → `s.css`

### Security

- `ConfigWebSecurity` extends `WebSecurityConfigurerAdapter` (deprecated in newer Spring Security)
- `/admin/**` requires ADMIN role; credentials configured in `application.properties`
- Static resources (`/b/**`, `/ckeditor/**`, `/css/**`, `/fonts/**`, `/img/**`, `/js/**`) bypass security

### Key Patterns

- `AdminController.@InitBinder` registers custom `PropertyEditorSupport` instances to bind form select values (integers) to domain objects (Municipality, Canton, District)
- `PageService.getPageByProperty(property, value, log)` builds SQL with the `property` parameter concatenated into the query — callers only pass hardcoded strings ("id", "name")
- Bot detection in `WebController.getPage()` checks user-agent for known crawler strings before incrementing view count
- `Scheduler` tweets a random page daily at 8:15 AM Europe/Luxembourg time via Twitter4j

### Geographic Hierarchy

District → Canton → Municipality → Page. The `PageMapper` reconstructs this nested relationship from joined query result columns (`dist_*`, `can_*`, `mun_*`).

### Deployment

Systemd service file: `tessyglodt_lu.service`

## Known Technical Debt & Quick Wins

### Dead Code to Remove
- `PageService.getSearchH2()` and the `switch(driverClassName)` in `WebController.getSearch()` — H2 is no longer used
- `PageService.registerUserDefinedFunctions()` and its endpoint `/admin/udf` — body is commented out
- `PageService.deleteAllPages()` — body is commented out
- `/admin/import` endpoint — body is commented out
- Large commented-out blocks in `WebController` (old photo endpoints), `Utils` (image resizing), `ConfigWebMvc`

### Deprecated APIs to Fix
- `MyHandlerInterceptor` extends `HandlerInterceptorAdapter` — implement `HandlerInterceptor` interface instead
- `ConfigWebSecurity` extends `WebSecurityConfigurerAdapter` — use `SecurityFilterChain` bean (required for Spring Boot 3.x)
- `new Object[] {}` in JdbcTemplate calls throughout `PageService` — use varargs directly

### Security
- Credentials are committed in `application.properties` (`spring.security.user.password`, Twitter secrets) — move to environment variables or a gitignored `application-local.properties`

### Missing Essentials
- No DB schema files in the repo — export with `pg_dump -s tessyglodt > schema.sql` and commit
- No test classes exist

### Twitter4j / Scheduler
- The Twitter/X API has fundamentally changed. The daily tweet scheduler (`Scheduler.java`, 8:15 AM) likely no longer works. Consider removing or replacing with Bluesky/Mastodon.

### Spring Boot Upgrade Path (2.7 → 3.x)
Spring Boot 2.7 is EOL. Main migration steps: `javax.*` → `jakarta.*` imports, `WebSecurityConfigurerAdapter` → `SecurityFilterChain` bean. Straightforward for this codebase (no JPA, no complex security).

## Migration Assessments

### SQLite Migration (assessed March 2025)

**Verdict: Medium-high effort (~30-50 hours), with a critical feature loss (full-text search).**

PostgreSQL-specific features in use:
- `?::date` casts in insert/update queries — trivial to remove
- `now()` in `updateViewCount()` — replace with `CURRENT_TIMESTAMP`
- `slugify()` custom DB function in `CantonService`, `DistrictService`, `PageService` — must reimplement in Java
- `unaccent()` in search — must do accent stripping in Java
- `to_tsvector`/`to_tsquery`/`@@` full-text search — **no direct SQLite equivalent**. SQLite FTS5 exists but doesn't handle accents and behaves differently. This is the dealbreaker for a Luxembourgish content site.

Other concerns: SQLite is single-writer (concurrent writes block), BigDecimal precision loss with REAL type (store as TEXT), no schema files exist to port.

**Recommendation: Keep PostgreSQL.** It runs fine locally via Postgres.app, and the full-text search + `unaccent` + `slugify` combo is doing real work.

### Thymeleaf Migration (assessed March 2025)

24 templates using layout dialect, fragment composition, `sec:authorize`, `@InitBinder` form binding, inline JavaScript for Google Maps.

**Best option if migrating: htmx (~2-3 weeks, ~72-103 hours)**
- Controllers return HTML fragments, add `hx-get`/`hx-post`/`hx-target` attributes. Can migrate incrementally, one page at a time.
- Keeps: server-side rendering (SEO), Spring Security integration, CKEditor, form validation, no build toolchain needed. ~14KB gzipped.

**Overkill: Vue/React SPA (~4-5 weeks)**
- Would require building a full REST API, reimplement form binding, handle auth client-side, lose SEO. Solves problems this site doesn't have.

**Lateral move: Different template engine like Pebble/FreeMarker (~2-3 weeks)**
- Same architecture, different syntax. Doesn't simplify anything meaningful.

**Recommendation: Stay with Thymeleaf unless there's a strong reason to change.** If modernizing, htmx is the clear winner — incremental migration, no npm/webpack, keeps server-side rendering.

## Local Database Setup (Postgres.app on macOS)

Restoring a `pg_dump` backup:
```bash
# Add Postgres.app CLI tools to PATH (add to ~/.zshrc for permanence)
export PATH="/Applications/Postgres.app/Contents/Versions/latest/bin:$PATH"

# Create database and role
createdb tessyglodt
psql -d tessyglodt -c "CREATE ROLE tessyglodt WITH LOGIN PASSWORD 'tessyglodt';"
psql -d tessyglodt -c "GRANT ALL PRIVILEGES ON DATABASE tessyglodt TO tessyglodt;"

# Restore plain SQL format (.sql)
psql -d tessyglodt < /path/to/backup.sql

# Restore custom format (-Fc, .dump)
pg_restore -d tessyglodt /path/to/backup.dump

# If backup was made by a different user
pg_restore --no-owner --no-privileges -d tessyglodt /path/to/backup.dump

# Grant permissions after restore
psql -d tessyglodt -c "GRANT ALL ON ALL TABLES IN SCHEMA public TO tessyglodt;"
psql -d tessyglodt -c "GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO tessyglodt;"
```
