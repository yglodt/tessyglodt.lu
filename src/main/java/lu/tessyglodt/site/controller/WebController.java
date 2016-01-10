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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.service.CantonService;
import lu.tessyglodt.site.service.DistrictService;
import lu.tessyglodt.site.service.MunicipalityService;
import lu.tessyglodt.site.service.PageService;

@Controller
@EnableAutoConfiguration
public class WebController {

	final static Logger			logger	= LoggerFactory.getLogger(WebController.class);

	@Autowired
	private PageService			pageService;

	@Autowired
	private MunicipalityService	municipalityService;

	@Autowired
	private CantonService		cantonService;

	@Autowired
	private DistrictService		districtService;

	@Value("${spring.datasource.driverClassName}")
	private String				driverClassName;

	@RequestMapping(value = { "/", "/index.html" }, method = RequestMethod.GET)
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

	@RequestMapping(value = { "/page/{name}", "/page/{name}.html" }, method = RequestMethod.GET)
	public String getPage(@PathVariable("name") final String name, final Model model, HttpServletRequest request) {

		String ua = request.getHeader("user-agent");
		boolean log = true;

		if (ua != null) {
			ua = ua.toLowerCase();
			if ((!ua.contains("bot")) && (!ua.contains("spider")) && (!ua.contains("slurp")) && (!ua.contains("facebookexternalhit"))) {
				pageService.updateViewCount(name);
			} else {
				// logger.debug("Not updating viewCount for " + name + " since
				// client is a bot: " + ua);
				log = false;
			}
		}

		model.addAttribute("page", pageService.getPageByProperty("name", name, log));

		return "page";
	}

	@RequestMapping(value = { "/kaart", "/kaart.html" }, method = RequestMethod.GET)
	public String getMap(final Model model) {

		model.addAttribute("pageInfos", pageService.getPagesInfo());

		return "map";
	}

	@RequestMapping(value = { "/apropos", "/apropos.html" }, method = RequestMethod.GET)
	public String getAbout(final Model model) {

		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		model.addAttribute("randomPage", pageService.getRandomPage());

		return "about";
	}

	@RequestMapping(value = { "/auteur", "/auteur.html" }, method = RequestMethod.GET)
	public String getAuthor(final Model model) {

		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));
		model.addAttribute("randomPage", pageService.getRandomPage());

		return "author";
	}

	@RequestMapping(value = "/sich", method = RequestMethod.GET)
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

	@RequestMapping(value = "/canton/{name}", method = RequestMethod.GET)
	public String getByCanton(final Model model, @PathVariable(value = "name") final String name) {

		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByCanton(name));
		model.addAttribute("name", cantonService.getCantonBySlugifiedName(name)
				.getName());
		model.addAttribute("title", "Kanton");

		return "pagelistbymcd";
	}

	@RequestMapping(value = "/district/{name}", method = RequestMethod.GET)
	public String getByDistrict(final Model model, @PathVariable(value = "name") final String name) {

		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByDistrict(name));
		model.addAttribute("name",
				districtService.getDistrictBySlugifiedName(name).getName());
		model.addAttribute("title", "Distrikt");

		return "pagelistbymcd";
	}

	@RequestMapping(value = "/stats", method = RequestMethod.GET)
	public String getStats(final Model model) {
		model.addAttribute("pages", pageService.getStats());
		return "stats";
	}

	@ResponseBody
	@RequestMapping(value = "/feed/nei.xml", method = RequestMethod.GET)
	public void getFeedNewestPages(HttpServletResponse response) throws IOException, FeedException {

		List<Page> pages = pageService.getNewestPages(10, true);
		SyndFeed feed = Utils.createFeed("Nei Texter", pages);

		response.setContentType("application/atom+xml");
		response.setCharacterEncoding("UTF-8");
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, response.getWriter());
	}

	@ResponseBody
	@RequestMapping(value = "/feed/alles.xml", method = RequestMethod.GET)
	public void getFeedAllPages(HttpServletResponse response) throws IOException, FeedException {

		List<Page> pages = pageService.getPages();

		SyndFeed feed = Utils.createFeed("All d'Texter", pages);

		response.setContentType("application/atom+xml");
		response.setCharacterEncoding("UTF-8");
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, response.getWriter());
	}

}
