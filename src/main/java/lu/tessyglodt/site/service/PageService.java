package lu.tessyglodt.site.service;

import java.time.format.DateTimeFormatter;
import java.util.Date;
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

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.data.PageMapper;

@Component
public class PageService {

	final static Logger						logger			= LoggerFactory.getLogger(PageService.class);

	private final Random					randomGenerator	= new Random();

	private final JdbcTemplate				jdbcTemplate;

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
		return rows;
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

	public List<Map<String, Object>> getSearchPostgreSQL(final String q) {
		/*
		 * http://blog.lostpropertyhq.com/postgres-full-text-search-is-good-
		 * enough /
		 * http://stackoverflow.com/questions/10027996/postgres-fulltext-index
		 */
		final String sql = "select "
				+ "id, name, title "
				+ "from (select "
				+ "p.id as id, p.name as name, p.title as title, to_tsvector(unaccent(p.content)) "
				+ "as document from page p where p.published) p_search "
				+ "where p_search.document @@ websearch_to_tsquery(unaccent(?))";

		return jdbcTemplate.queryForList(sql, q);

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

	public List<Map<String, Object>> getStats(final boolean includeUnpublished) {
		final String sql = "select name, title, view_count, date_last_view from page "
				+ (includeUnpublished ? "" : "where published ")
				+ "order by view_count desc";
		return jdbcTemplate.queryForList(sql);
	}

}
