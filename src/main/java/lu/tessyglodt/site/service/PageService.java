package lu.tessyglodt.site.service;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.NearbyPage;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.data.PageMapper;
import lu.tessyglodt.site.data.PageNeighbours;
import lu.tessyglodt.site.data.SearchHit;
import lu.tessyglodt.site.data.SearchResults;

@Component
public class PageService {

	final static Logger						logger			= LoggerFactory.getLogger(PageService.class);

	private final Random					randomGenerator	= new Random();

	// ts_headline marks the hits and the gaps between fragments with control characters, which can't
	// occur in the texts, so the excerpt can be escaped as a whole before they become HTML
	private static final String				HEADLINE_OPTIONS	= "MaxFragments=2, MaxWords=45, MinWords=30, FragmentDelimiter=\u0004, StartSel=\u0002, StopSel=\u0003";

	private static final String				ELLIPSIS			= "<span class=\"gap\">…</span>";

	private final JdbcTemplate				jdbcTemplate;

	// Previous and next page for each page name. Filled whenever getPagesInfo() loads the list,
	// cleared by insert() and update() together with the "page" cache.
	private volatile Map<String, PageNeighbours>	pageNeighbours;

	public PageService(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Cacheable(value = "page", key = "#root.methodName")
	public Long countPages() {
		return jdbcTemplate.queryForObject("select count(id) from page", Long.class);
	}

	@Cacheable(value = "page", key = "#root.methodName")
	public List<Page> getPagesInfo() {
		// logger.debug("");
		final String sql = "select id, name, title, "
				+ "latitude, longitude, '' as content, "
				+ "0 as dist_id, '' as dist_name, 0 as can_id, "
				+ "'' as can_name, 0 as mun_id, '' as mun_name, "
				+ "date_published, published, site, type "
				+ "from page where published order by title asc";
		final List<Page> rows = jdbcTemplate.query(sql, new PageMapper());

		final Map<String, PageNeighbours> neighbours = new HashMap<>();
		for (int i = 0; i < rows.size(); i++) {
			neighbours.put(rows.get(i).getName(), new PageNeighbours(
					i > 0 ? rows.get(i - 1) : null,
					i < rows.size() - 1 ? rows.get(i + 1) : null));
		}
		pageNeighbours = neighbours;

		return rows;
	}

	// Previous and next published page in alphabetical order, null for hidden or unknown pages
	public PageNeighbours getPageNeighbours(final String name) {
		Map<String, PageNeighbours> neighbours = pageNeighbours;
		if (neighbours == null) {
			// Only after a page was saved. A call on this instance skips the cache, so this reloads the list.
			getPagesInfo();
			neighbours = pageNeighbours;
		}
		return neighbours == null ? null : neighbours.get(name);
	}

	// @Cacheable(value = "page", key = "#root.methodName")
	public List<Page> getPages() {
		final String sql = "select id, name, title, "
				+ "latitude, longitude, content, 0 as dist_id, "
				+ "'' as dist_name, 0 as can_id, '' as can_name, "
				+ "0 as mun_id, '' as mun_name, date_published, "
				+ "published, site, type from page where published order by title asc";
		final List<Page> rows = jdbcTemplate.query(sql, new PageMapper());
		return rows;
	}

	public Page getRandomPage() {
		final List<Page> allPages = getPagesInfo();
		final Page randomPage = allPages.get(randomGenerator.nextInt(allPages.size()));
		return getPageByProperty("name", randomPage.getName(), false);
	}

	// Nearest published pages. Treats the map as flat, which is accurate to
	// a few metres over the size of Luxembourg (1° of latitude = 111.2 km).
	@Cacheable(value = "page", key = "#root.methodName + #p0.id + '_' + #p1")
	public List<NearbyPage> getNearbyPages(final Page page, final int count) {
		final String sql = "select name, title, "
				+ "111.2 * sqrt(power(latitude - ?, 2) + power((longitude - ?) * cos(radians(?)), 2)) as distance "
				+ "from page where published and id <> ? order by distance limit ?";
		return jdbcTemplate.query(sql, (rs, rowNum) -> new NearbyPage(rs.getString("name"), rs.getString("title"), rs.getDouble("distance")),
				page.getLatitude(), page.getLongitude(), page.getLatitude(), page.getId(), count);
	}

	// Only returns published pages. "joins" may be empty, "condition" is
	// and-ed with the published filter, "orderBy" may include a limit.
	private List<Page> getPagesWithWhere(final String joins, final String condition, final String orderBy, final Object[] params, final boolean fullContent) {
		final String sql = "select p.id, p.name, " + "p.title, "
				+ "0 as latitude, " + "0 as longitude, "
				+ ((fullContent) ? "p.content, " : "'' as content, ")
				+ "0 as dist_id, '' as dist_name, "
				+ "0 as can_id, '' as can_name, 0 as mun_id, "
				+ "'' as mun_name, p.date_published, p.published, "
				+ "p.site, p.type from page p " + joins
				+ " where p.published" + (condition.isEmpty() ? "" : " and " + condition)
				+ " " + orderBy;
		final List<Page> rows = jdbcTemplate.query(sql, new PageMapper(), params);
		return rows;

	}

	@Cacheable(value = "accessInfo", key = "#root.methodName + #p0")
	public List<Page> getLastReadPages(final int i) {
		return getPagesWithWhere("", "", "order by date_last_view desc limit ?", new Object[] { i }, false);
	}

	@Cacheable(value = "accessInfo", key = "#root.methodName + #p0 + #p1")
	public List<Page> getNewestPages(final int i, final boolean fullContent) {
		// return getPagesWithWhere("order by date_published desc limit ?", new
		// Object[] { i });
		return getPagesWithWhere("", "", "order by date_created desc limit ?", new Object[] { i }, fullContent);
	}

	@Cacheable(value = "accessInfo", key = "#root.methodName + #p0")
	public List<Page> getMostReadPages(final int i) {
		return getPagesWithWhere("", "", "order by view_count desc limit ?", new Object[] { i }, false);
	}

	@CacheEvict(value = "accessInfo", allEntries = true)
	@Cacheable(value = "page", key = "#root.methodName + #p0 + #p1")
	public Page getPageByProperty(final String property, final String value, final boolean log) {
		final String sql = "select p.*, d.id as dist_id, "
				+ "d.name as dist_name, c.id as can_id, "
				+ "c.name as can_name, m.id as mun_id, "
				+ "m.name as mun_name from page p "
				+ "left join municipality m on m.id = p.municipality "
				+ "left join canton c on c.id = m.canton "
				+ "left join district d on d.id = c.district where p." + property
				+ " = ?";
		if (log) {
			logger.debug("key: " + property + ", value: " + value);
		}
		final Page o = jdbcTemplate.queryForObject(sql, new PageMapper(), value);
		return o;
	}

	// Full-text search over page.search (title weighted above text, see tools/search/search-index.sql),
	// best matches first, with excerpts around the hits for the requested slice only
	public SearchResults search(final String q, final int offset, final int limit) {
		final String sql = "select name, title, total, plain, "
				+ "ts_headline('public.english_unaccent', plain, q, ?) as excerpt "
				+ "from (select name, title, content, q, ts_rank(search, q) as rank, count(*) over () as total "
				+ "from page, websearch_to_tsquery('public.english_unaccent', ?) q "
				+ "where published and search @@ q "
				+ "order by rank desc, title limit ? offset ?) hits, "
				+ "lateral (select trim(regexp_replace(regexp_replace(content, '<[^>]+>', ' ', 'g'), '\\s+', ' ', 'g')) as plain) t "
				+ "order by rank desc, title";
		final int[] total = { 0 };
		final List<SearchHit> hits = jdbcTemplate.query(sql, (rs, rowNum) -> {
			total[0] = rs.getInt("total");
			return new SearchHit(rs.getString("name"), rs.getString("title"), excerptHtml(rs.getString("excerpt"), rs.getString("plain")));
		}, HEADLINE_OPTIONS, q, limit, offset);
		return new SearchResults(hits, total[0]);
	}

	// The excerpt as HTML, with an ellipsis wherever text was left out: before it unless it starts the
	// text, between the fragments, and after it unless it ends the text
	private static String excerptHtml(final String headline, final String plain) {
		final String excerpt = headline.strip();
		final String bare = excerpt.replaceAll("[\u0002\u0003]", "");
		final String[] fragments = bare.split("\u0004");
		final String html = HtmlUtils.htmlEscape(HtmlUtils.htmlUnescape(excerpt), "UTF-8")
				.replace("\u0002", "<mark>").replace("\u0003", "</mark>")
				.replaceAll("\\s*\u0004\\s*", " " + ELLIPSIS + " ");
		return (plain.startsWith(fragments[0].strip()) ? "" : ELLIPSIS + " ") + html
				+ (plain.endsWith(fragments[fragments.length - 1].strip()) ? "" : " " + ELLIPSIS);
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public List<Page> getPagesByCanton(final String cantonName) {
		return getPagesWithWhere(
				"left join municipality m on m.id = p.municipality "
						+ "left join canton c on c.id = m.canton",
				"slugify(c.name) = ?", "order by title asc",
				new Object[] { cantonName }, false);
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public List<Page> getPagesByDistrict(final String districtName) {
		return getPagesWithWhere(
				"left join municipality m on m.id = p.municipality "
						+ "left join canton c on c.id = m.canton "
						+ "left join district d on d.id = c.district",
				"slugify(d.name) = ?", "order by title asc",
				new Object[] { districtName }, false);
	}

	@CacheEvict(value = { "page", "accessInfo" }, allEntries = true)
	public void insert(final Page page) {
		final String sql = "insert into page "
				+ "(id, name, title, latitude, longitude, content, "
				+ "municipality, date_published, published) values "
				+ "(?,?,?,?,?,?,?,?::date,?)";

		final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String dateAsString = null;
		if (page.getDatePublished() != null) {
			dateAsString = page.getDatePublished().format(df);
		}

		jdbcTemplate.update(sql, UUID.randomUUID().toString().replace("-", ""),
				page.getName(), page.getTitle(), page.getLatitude(),
				page.getLongitude(), page.getContent(),
				Utils.getValue(page.getMunicipality()),
				dateAsString, page.isPublished());
		pageNeighbours = null;
	}

	@CacheEvict(value = { "page", "accessInfo" }, allEntries = true)
	public void update(final Page page) {
		final String sql = "update page set name = ?, title = ?, "
				+ "latitude = ?, longitude = ?, content = ?, "
				+ "municipality = ?, date_published = ?::date, "
				+ "published = ?, date_modified = ? where id = ?";

		final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String dateAsString = null;
		if (page.getDatePublished() != null) {
			dateAsString = page.getDatePublished().format(df);
		}

		jdbcTemplate.update(sql, page.getName(), page.getTitle(),
				page.getLatitude(), page.getLongitude(), page.getContent(),
				Utils.getValue(page.getMunicipality()),
				dateAsString, page.isPublished(), new Date(),
				page.getId());
		pageNeighbours = null;
	}

	public void updateViewCount(final String name) {
		final String sql = "update page "
				+ "set date_last_view = now(), "
				+ "view_count = view_count + 1 "
				+ "where name = ? and published";
		jdbcTemplate.update(sql, name);
	}

	// For the admin bar: hidden pages are not listed anywhere else
	@Cacheable(value = "page", key = "#root.methodName")
	public List<Map<String, Object>> getUnpublishedPages() {
		return jdbcTemplate.queryForList("select name, title from page where not published order by title asc");
	}

	// For sitemap.xml: published pages with their last change (edit date, or publish date if never edited)
	@Cacheable(value = "page", key = "#root.methodName")
	public List<Map<String, Object>> getSitemapEntries() {
		return jdbcTemplate.queryForList("select name, coalesce(date_modified::date, date_published) as lastmod "
				+ "from page where published order by name asc");
	}

	public List<Map<String, Object>> getStats(final boolean includeUnpublished) {
		final String sql = "select name, title, view_count, date_last_view from page "
				+ (includeUnpublished ? "" : "where published ")
				+ "order by view_count desc";
		return jdbcTemplate.queryForList(sql);
	}

}
