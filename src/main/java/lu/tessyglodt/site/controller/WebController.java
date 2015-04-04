package lu.tessyglodt.site.controller;

import lu.tessyglodt.site.service.CantonService;
import lu.tessyglodt.site.service.DistrictService;
import lu.tessyglodt.site.service.MunicipalityService;
import lu.tessyglodt.site.service.PageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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

	@RequestMapping(value = { "/", "/index.html" }, method = RequestMethod.GET)
	public String getIndex(final Model model) {

		model.addAttribute("pages", pageService.getPagesInfo());
		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("randomPage", pageService.getRandomPage());
		model.addAttribute("newestPages", pageService.getNewestPages(5));
		model.addAttribute("lastReadPages", pageService.getLastReadPages(5));
		model.addAttribute("mostReadPages", pageService.getMostReadPages(5));

		return "index";
	}

	@RequestMapping(value = { "/page/{name}", "/page/{name}.html" }, method = RequestMethod.GET)
	public String getPage(@PathVariable("name") final String name, final Model model) {

		pageService.updateViewCount(name);
		model.addAttribute("page", pageService.getPage("name", name));

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
			model.addAttribute("pages", pageService.getSearch(q));
		}

		return "search";
	}

	@RequestMapping(value = "/canton/{name}", method = RequestMethod.GET)
	public String getByCanton(final Model model, @PathVariable(value = "name") final String name) {

		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByCanton(name));
		model.addAttribute("name", cantonService.getCantonBySlugifiedName(name).getName());
		model.addAttribute("title", "Kanton");

		return "pagelistbymcd";
	}

	@RequestMapping(value = "/district/{name}", method = RequestMethod.GET)
	public String getByDistrict(final Model model, @PathVariable(value = "name") final String name) {

		model.addAttribute("cantons", cantonService.getCantons());
		model.addAttribute("districts", districtService.getDistricts());
		model.addAttribute("pages", pageService.getPagesByDistrict(name));
		model.addAttribute("name", districtService.getDistrictBySlugifiedName(name).getName());
		model.addAttribute("title", "Distrikt");

		return "pagelistbymcd";
	}

	@RequestMapping(value = "/stats", method = RequestMethod.GET)
	public String getStats(final Model model) {

		model.addAttribute("pages", pageService.getStats());

		return "stats";
	}

}
