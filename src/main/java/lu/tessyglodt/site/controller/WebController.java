package lu.tessyglodt.site.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.service.CantonService;
import lu.tessyglodt.site.service.DistrictService;
import lu.tessyglodt.site.service.PageService;

@Controller
@EnableAutoConfiguration
public class WebController {

	final static Logger		logger	= LoggerFactory.getLogger(WebController.class);

	@Autowired
	private PageService		pageService;

	@Autowired
	private CantonService	cantonService;

	@Autowired
	private DistrictService	districtService;

	@Value("${spring.datasource.driverClassName}")
	private String			driverClassName;

	@GetMapping(value = { "/", "/index.html" })
	public String getIndex(final Model model) {
		model.addAttribute("pages", pageService.getPagesInfo());
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("randomPage", pageService.getRandomPage());
		model.addAttribute("newestPages", pageService.getNewestPages(5, false));
		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		return "index";
	}

	@GetMapping(value = { "/page/{name}", "/page/{name}.html" })
	public String getPage(@PathVariable("name") final String name, final Model model, HttpServletRequest request) {

		String ua = request.getHeader("user-agent");
		boolean log = true;

		if (ua != null) {
			ua = ua.toLowerCase();
			if ((!ua.contains("bot")) &&
					(!ua.contains("spider")) &&
					(!ua.contains("slurp")) &&
					(!ua.contains("scrap")) &&
					(!ua.contains("netcraft")) &&
					(!ua.contains("crawl")) &&
					(!ua.contains("facebookexternalhit"))) {
				pageService.updateViewCount(name);
			} else {
				// logger.debug("Not updating viewCount for " + name + " since
				// client is a bot: " + ua);
				log = false;
			}
		}

		Page page = pageService.getPageByProperty("name", name, log);

		model.addAttribute("page", page);

		// logger.debug(page.getTweet());
		// pageService.tweetPage(page);

		return "page";
	}

	@GetMapping(value = { "/kaart", "/kaart.html" })
	public String getMap(final Model model) {
		model.addAttribute("pageInfos", pageService.getPagesInfo());
		return "map";
	}

	@GetMapping(value = { "/apropos", "/apropos.html" })
	public String getAbout(final Model model) {
		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		model.addAttribute("randomPage", pageService.getRandomPage());
		return "about";
	}

	@GetMapping(value = { "/auteur", "/auteur.html" })
	public String getAuthor(final Model model) {
		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		model.addAttribute("randomPage", pageService.getRandomPage());
		return "author";
	}

	@GetMapping(value = "/sich")
	public String getSearch(final Model model, @RequestParam(value = "q", required = false) final String q) {

		if (!StringUtils.isEmpty(q)) {
			logger.debug("Searching for \"" + q + "\"");

			switch (driverClassName) {
				case "org.h2.Driver":
					model.addAttribute("pages", pageService.getSearchH2(q));
					break;
				case "org.postgresql.Driver":
					model.addAttribute("pages", pageService.getSearchPostgreSQL(q));
					break;
			}

		}

		return "search";
	}

	@GetMapping(value = "/canton/{name}")
	public String getByCanton(final Model model, @PathVariable(value = "name") final String name) {
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByCanton(name));
		model.addAttribute("name", cantonService.getCantonBySlugifiedName(name).getName());
		model.addAttribute("title", "Kanton");
		return "pagelistbymcd";
	}

	@GetMapping(value = "/district/{name}")
	public String getByDistrict(final Model model, @PathVariable(value = "name") final String name) {
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByDistrict(name));
		model.addAttribute("name", districtService.getDistrictBySlugifiedName(name).getName());
		model.addAttribute("title", "Distrikt");
		return "pagelistbymcd";
	}

	@GetMapping(value = "/stats")
	public String getStats(final Model model) {
		model.addAttribute("pages", pageService.getStats());
		return "stats";
	}

	@ResponseBody
	@GetMapping(value = "/feed/nei.xml")
	public void getFeedNewestPages(HttpServletResponse response) throws IOException, FeedException {

		List<Page> pages = pageService.getNewestPages(10, true);
		SyndFeed feed = Utils.createFeed("Nei Texter", pages);

		response.setContentType("application/atom+xml");
		response.setCharacterEncoding("UTF-8");
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, response.getWriter());
	}

	@ResponseBody
	@GetMapping(value = "/feed/alles.xml")
	public void getFeedAllPages(HttpServletResponse response) throws IOException, FeedException {

		List<Page> pages = pageService.getPages();

		SyndFeed feed = Utils.createFeed("All d'Texter", pages);

		response.setContentType("application/atom+xml");
		response.setCharacterEncoding("UTF-8");
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, response.getWriter());
	}

	@ResponseBody
	@GetMapping(value = "/robots.txt")
	public String getRobots() {
		return "";
	}

}
