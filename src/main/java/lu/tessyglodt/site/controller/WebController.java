package lu.tessyglodt.site.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Canton;
import lu.tessyglodt.site.data.District;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.data.PageNeighbours;
import lu.tessyglodt.site.service.CantonService;
import lu.tessyglodt.site.service.DistrictService;
import lu.tessyglodt.site.service.PageService;

@Controller
// @EnableAutoConfiguration
public class WebController {

	private static final String			BASE_URL	= "https://www.tessyglodt.lu";

	final static Logger					logger	= LoggerFactory.getLogger(WebController.class);

	private final PageService			pageService;

	private final CantonService			cantonService;

	private final DistrictService		districtService;

	private final HttpServletRequest	request;

	public WebController(final PageService pageService, final CantonService cantonService, final DistrictService districtService, final HttpServletRequest request) {
		this.pageService = pageService;
		this.cantonService = cantonService;
		this.districtService = districtService;
		this.request = request;
	}

	@GetMapping(value = { "/", "/index.html" })
	public String getIndex(final Model model) {
		model.addAttribute("req", request);

		model.addAttribute("pagesByInitial", Utils.groupByInitial(pageService.getPagesInfo()));
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("randomPage", pageService.getRandomPage());
		model.addAttribute("newestPages", pageService.getNewestPages(5, false));
		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		return "index";
	}

	@GetMapping(value = { "/page/{name}", "/page/{name}.html" })
	public String getPage(@PathVariable("name") final String name, final Model model, final HttpServletRequest request) {
		model.addAttribute("req", request);

		String ua = request.getHeader("user-agent");
		boolean isBot = false;

		if (ua != null) {
			ua = ua.toLowerCase();
			isBot = ua.contains("bot") ||
					ua.contains("spider") ||
					ua.contains("slurp") ||
					ua.contains("scrap") ||
					ua.contains("netcraft") ||
					ua.contains("crawl") ||
					ua.contains("facebookexternalhit");
		}

		// Must run before getPageByProperty(), which evicts the "last read"/"most read" caches.
		// Only touches published pages, so unknown and hidden pages are not counted.
		if (ua != null && !isBot) {
			pageService.updateViewCount(name);
		}

		// Throws EmptyResultDataAccessException (-> 404) for unknown pages
		final Page page = pageService.getPageByProperty("name", name, !isBot);

		if (!page.isPublished() && !request.isUserInRole("ADMIN")) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		model.addAttribute("page", page);
		model.addAttribute("nearbyPages", pageService.getNearbyPages(page, 5));

		// Previous and next page in alphabetical order, like turning a page in the book.
		// Hidden pages aren't in the map, so they get no links.
		final PageNeighbours neighbours = pageService.getPageNeighbours(page.getName());
		if (neighbours != null) {
			model.addAttribute("previousPage", neighbours.previous());
			model.addAttribute("nextPage", neighbours.next());
		}

		return "page";
	}

	@GetMapping(value = { "/kaart", "/kaart.html" })
	public String getMap(final Model model) {
		model.addAttribute("req", request);

		model.addAttribute("pageInfos", pageService.getPagesInfo());
		return "map";
	}

	@GetMapping(value = { "/apropos", "/apropos.html" })
	public String getAbout(final Model model) {
		model.addAttribute("req", request);

		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		model.addAttribute("randomPage", pageService.getRandomPage());
		return "about";
	}

	// Not linked from the site; exists for the Meta app settings (privacy policy URL)
	@GetMapping(value = "/dateschutz")
	public String getPrivacy() {
		return "privacy";
	}

	@GetMapping(value = { "/auteur", "/auteur.html" })
	public String getAuthor(final Model model) {
		model.addAttribute("req", request);

		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		model.addAttribute("randomPage", pageService.getRandomPage());
		return "author";
	}

	@GetMapping(value = "/sich")
	public String getSearch(final Model model, @RequestParam(value = "q", required = false) final String q) {
		model.addAttribute("req", request);

		if (!StringUtils.isEmpty(q)) {
			logger.debug("Searching for \"" + q + "\"");

			model.addAttribute("pages", pageService.getSearchPostgreSQL(q));

		}

		return "search";
	}

	@GetMapping(value = "/canton/{name}")
	public String getByCanton(final Model model, @PathVariable(value = "name") final String name) {
		model.addAttribute("req", request);

		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByCanton(name));
		model.addAttribute("name", cantonService.getCantonBySlugifiedName(name).getName());
		model.addAttribute("title", "Kanton");
		return "pagelistbymcd";
	}

	@GetMapping(value = "/district/{name}")
	public String getByDistrict(final Model model, @PathVariable(value = "name") final String name) {
		model.addAttribute("req", request);

		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByDistrict(name));
		model.addAttribute("name", districtService.getDistrictBySlugifiedName(name).getName());
		model.addAttribute("title", "Distrikt");
		return "pagelistbymcd";
	}

	@GetMapping(value = "/stats")
	public String getStats(final Model model) {
		model.addAttribute("req", request);

		model.addAttribute("pages", pageService.getStats(request.isUserInRole("ADMIN")));
		return "stats";
	}

	@ResponseBody
	@GetMapping(value = "/feed/nei.xml")
	public void getFeedNewestPages(final HttpServletResponse response) throws IOException, FeedException {
		final List<Page> pages = pageService.getNewestPages(10, true);
		final SyndFeed feed = Utils.createFeed("Nei Texter", pages);

		response.setContentType("application/atom+xml");
		response.setCharacterEncoding("UTF-8");
		final SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, response.getWriter());
	}

	@ResponseBody
	@GetMapping(value = "/feed/alles.xml")
	public void getFeedAllPages(final HttpServletResponse response) throws IOException, FeedException {

		final List<Page> pages = pageService.getPages();

		final SyndFeed feed = Utils.createFeed("All d'Texter", pages);

		response.setContentType("application/atom+xml");
		response.setCharacterEncoding("UTF-8");
		final SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, response.getWriter());
	}

	@ResponseBody
	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getRobots() {
		return "User-agent: *\n"
				+ "Disallow: /admin/\n"
				+ "Disallow: /login\n"
				+ "Disallow: /sich\n"
				+ "\n"
				+ "Sitemap: " + BASE_URL + "/sitemap.xml\n";
	}

	@ResponseBody
	@GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String getSitemap() {
		final StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		for (final String path : List.of("/", "/kaart", "/apropos", "/auteur")) {
			appendSitemapUrl(xml, path, null);
		}
		for (final Canton canton : cantonService.getCantons()) {
			appendSitemapUrl(xml, "/canton/" + UriUtils.encodePathSegment(canton.getSlug(), "UTF-8"), null);
		}
		for (final District district : districtService.getDistricts()) {
			appendSitemapUrl(xml, "/district/" + UriUtils.encodePathSegment(district.getSlug(), "UTF-8"), null);
		}
		for (final Map<String, Object> entry : pageService.getSitemapEntries()) {
			final Object lastmod = entry.get("lastmod");
			appendSitemapUrl(xml, "/page/" + UriUtils.encodePathSegment((String) entry.get("name"), "UTF-8"), lastmod == null ? null : lastmod.toString());
		}
		xml.append("</urlset>\n");
		return xml.toString();
	}

	private static void appendSitemapUrl(final StringBuilder xml, final String path, final String lastmod) {
		xml.append("<url><loc>").append(HtmlUtils.htmlEscape(BASE_URL + path)).append("</loc>");
		if (lastmod != null) {
			xml.append("<lastmod>").append(lastmod).append("</lastmod>");
		}
		xml.append("</url>\n");
	}
}
