package lu.tessyglodt.site.service;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.TweetData;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.TwitterTemplateCreator;
import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.data.PageMapper;

@Component
public class PageService {

	final static Logger				logger			= LoggerFactory.getLogger(PageService.class);

	private final Random			randomGenerator	= new Random();

	@Autowired
	private JdbcTemplate			jdbcTemplate;

	@Autowired
	private TwitterTemplateCreator	twitterCreator;

	@Cacheable(value = "page", key = "#root.methodName")
	public Long countPages() {
		return jdbcTemplate.queryForObject("select count(id) from page", Long.class);
	}

	@Cacheable(value = "page", key = "#root.methodName")
	public List<Page> getPagesInfo() {
		final String sql = "select id, name, title, "
				+ "latitude, longitude, '' as content, "
				+ "0 as dist_id, '' as dist_name, 0 as can_id, "
				+ "'' as can_name, 0 as mun_id, '' as mun_name, "
				+ "date_published, published, site, type "
				+ "from page order by title asc";
		final List<Page> rows = jdbcTemplate.query(sql, new Object[] {}, new PageMapper());
		return rows;
	}

	// @Cacheable(value = "page", key = "#root.methodName")
	public List<Page> getPages() {
		final String sql = "select id, name, title, "
				+ "latitude, longitude, content, 0 as dist_id, "
				+ "'' as dist_name, 0 as can_id, '' as can_name, "
				+ "0 as mun_id, '' as mun_name, date_published, "
				+ "published, site, type from page " + "order by title asc";
		final List<Page> rows = jdbcTemplate.query(sql, new Object[] {},
				new PageMapper());
		return rows;
	}

	public Page getRandomPage() {
		List<Page> allPages = getPagesInfo();
		Page randomPage = allPages.get(randomGenerator.nextInt(allPages.size()));
		return getPageByProperty("name", randomPage.getName(), false);
	}

	private List<Page> getPagesWithWhere(final String clause, final Object[] params, boolean fullContent) {
		final String sql = "select p.id, p.name, " + "p.title, "
				+ "0 as latitude, " + "0 as longitude, "
				+ ((fullContent) ? "p.content, " : "'' as content, ")
				+ "0 as dist_id, '' as dist_name, "
				+ "0 as can_id, '' as can_name, 0 as mun_id, "
				+ "'' as mun_name, p.date_published, p.published, "
				+ "p.site, p.type from page p " + clause;
		final List<Page> rows = jdbcTemplate.query(sql, params, new PageMapper());
		return rows;

	}

	@Cacheable(value = "accessInfo", key = "#root.methodName + #p0")
	public List<Page> getLastReadPages(final int i) {
		return getPagesWithWhere("order by date_last_view desc limit ?", new Object[] { i }, false);
	}

	@Cacheable(value = "accessInfo", key = "#root.methodName + #p0 + #p1")
	public List<Page> getNewestPages(final int i, boolean fullContent) {
		// return getPagesWithWhere("order by date_published desc limit ?", new
		// Object[] { i });
		return getPagesWithWhere("order by date_created desc limit ?", new Object[] { i }, fullContent);
	}

	@Cacheable(value = "accessInfo", key = "#root.methodName + #p0")
	public List<Page> getMostReadPages(final int i) {
		return getPagesWithWhere("order by view_count desc limit ?", new Object[] { i }, false);
	}

	@CacheEvict(value = "accessInfo", allEntries = true)
	@Cacheable(value = "page", key = "#root.methodName + #p0 + #p1")
	public Page getPageByProperty(final String property, final String value, boolean log) {
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
		final Page o = jdbcTemplate.queryForObject(sql, new Object[] { value }, new PageMapper());
		return o;
	}

	public List<Page> getSearchH2(final String q) {
		final String sql = "SELECT * FROM FT_SEARCH_DATA(?, 0, 0)";

		final List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,
				new Object[] { q });

		final StringBuffer params = new StringBuffer();

		for (final Map<String, Object> temp : rows) {
			final Object[] id = (Object[]) temp.get("KEYS");
			final String idAsString = (String) id[0];
			params.append("'" + idAsString + "',");
		}

		String params2 = params.toString();

		if (params.length() > 0) {
			// I could not find a way to use an array as a query parameter with
			// jdbctemplate, that's why I use string concatenation. It has no
			// security impact since the original query with the search string
			// is done before, using a query parameter.
			params2 = params2.substring(0, params.length() - 1);
			return getPagesWithWhere("where p.id in (" + params2 + ") order by title asc", null, false);
		} else {
			return null;
		}

	}

	public List<Map<String, Object>> getSearchPostgreSQL(final String q) {
		/*
		 *
		 * http://blog.lostpropertyhq.com/postgres-full-text-search-is-good-
		 * enough /
		 *
		 * http://stackoverflow.com/questions/10027996/postgres-fulltext-index
		 */
		final String sql = "select "
				+ "id, name, title "
				+ "from (select "
				+ "p.id as id, p.name as name, p.title as title, to_tsvector(unaccent(p.content)) "
				+ "as document from page p) p_search "
				+ "where p_search.document @@ to_tsquery(unaccent(?))";

		return jdbcTemplate.queryForList(sql, new Object[] { q });

	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public List<Page> getPagesByCanton(final Integer cantonId) {
		return getPagesWithWhere("where p.canton = ? order by p.title asc", new Object[] { cantonId }, false);
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public List<Page> getPagesByCanton(final String cantonName) {
		return getPagesWithWhere(
				"left join municipality m on m.id = p.municipality "
						+ "left join canton c on c.id = m.canton "
						+ "where slugify(c.name) = ? order by title asc",
				new Object[] { cantonName }, false);
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public List<Page> getPagesByDistrict(final Integer districtId) {
		return getPagesWithWhere("where p.district = ? order by p.title asc",
				new Object[] { districtId }, false);
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public List<Page> getPagesByDistrict(final String districtName) {
		return getPagesWithWhere(
				"left join municipality m on m.id = p.municipality "
						+ "left join canton c on c.id = m.canton "
						+ "left join district d on d.id = c.district "
						+ "where slugify(d.name) = ? order by title asc",
				new Object[] { districtName }, false);
	}

	@CacheEvict(value = { "page", "accessInfo" }, allEntries = true)
	public void insert(final Page page) {
		final String sql = "insert into page "
				+ "(id, name, title, latitude, longitude, content, "
				+ "municipality, date_published, published) values "
				+ "(?,?,?,?,?,?,?,?::date,?)";

		DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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

		DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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

	public void deleteAllPages() {
		// this.jdbcTemplate.update("delete from page");
	}

	public void registerUserDefinedFunctions() {
		// this.jdbcTemplate.update("create alias if not exists slugify for
		// \"lu.tessyglodt.site.H2Functions.slugify\";");
		// this.jdbcTemplate.update("create alias if not exists ft_init for
		// \"org.h2.fulltext.fulltext.init\";");
	}

	/*
	 * public void backup() { try {
	 * Script.process("jdbc:h2:/data/tessyglodt_lu", "sa", "",
	 * "/data/database-backup-" + new
	 * SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".sql", "", "");
	 * } catch (final SQLException e) { logger.error(e.getMessage()); } }
	 *
	 * public void restore() { // java -cp //
	 * "$dir/h2-1.3.174.jar:$dir/slugify-2.1.2.jar:$dir/lu/tessyglodt/site/:$H2DRIVERS:$CLASSPATH"
	 * // org.h2.tools.RunScript -url jdbc:h2:/tmp/test2 -user sa -script //
	 * /tmp/db.sql
	 *
	 * // http://www.h2database.com/javadoc/org/h2/tools/RunScript.html
	 *
	 * try { RunScript.execute( "jdbc:h2:/data/restore-" + new
	 * SimpleDateFormat("yyyyMMddHHmmss").format(new Date()), "sa", "",
	 * "/data/file_to_restore.sql", null, false); } catch (final SQLException e)
	 * { logger.error(e.getMessage()); } }
	 */
	public void updateViewCount(final String name) {
		final String sql = "update page "
				+ "set date_last_view = now(), "
				+ "view_count = (select view_count from page where name = ?) + 1 "
				+ "where name = ?";
		jdbcTemplate.update(sql, name, name);
	}

	public List<Map<String, Object>> getStats() {
		final String sql = "select name, title, view_count, date_last_view from page order by view_count desc";
		return jdbcTemplate.queryForList(sql);
	}

	public Tweet tweetPage(Page page) {
		logger.debug("Tweeting " + page.getTweet());
		Twitter tt = twitterCreator.getTwitterTemplate();
		return tt.timelineOperations().updateStatus(new TweetData(page.getTweet()).atLocation(page.getLongitude().floatValue(), page.getLatitude().floatValue()));
	}

}
