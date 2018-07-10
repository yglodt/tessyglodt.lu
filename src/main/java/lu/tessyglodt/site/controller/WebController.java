package lu.tessyglodt.site.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Order;
import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.service.CantonService;
import lu.tessyglodt.site.service.DistrictService;
import lu.tessyglodt.site.service.OrderService;
import lu.tessyglodt.site.service.PageService;

@Controller
// @EnableAutoConfiguration
public class WebController {

	final static Logger		logger	= LoggerFactory.getLogger(WebController.class);

	@Autowired
	private PageService		pageService;

	@Autowired
	private CantonService	cantonService;

	@Autowired
	private DistrictService	districtService;

	@Autowired
	private OrderService	orderService;

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
	public String getPage(@PathVariable("name") final String name, final Model model, final HttpServletRequest request) {

		// final File oldPics = new File("/home/glodt/data/ville1/old");
		// final File[] oldPicsFiles = oldPics.listFiles(Utils.folderFilter());

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

		final Page page = pageService.getPageByProperty("name", name, log);

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
	@GetMapping(value = "/robots.txt")
	public String getRobots() {
		return "";
	}

	@GetMapping(value = "/blizzy")
	public String getOrderForm(final Model model, @RequestParam(value = "id", required = false) final String id) {
		model.addAttribute("order", new Order());
		model.addAttribute("parameters", orderService.getParameters());
		return "blizzy";
	}

	@PostMapping(value = "/blizzy")
	public String postOrderForm(@ModelAttribute final Order order) {
		orderService.insert(order);
		try {
			orderService.sendConfirmationEmail(order);
		} catch (UnsupportedEncodingException | MessagingException e) {
			logger.error("Error sending Mail: " + e.getMessage());
		}
		logger.debug("Inserted " + order);
		return "redirect:/blizzy?c=ok";
	}

	@ResponseBody
	@GetMapping(value = "/photo/l/{page}/{filename:.*}")
	public FileSystemResource getPic(
			@PathVariable("filename") final String fileName,
			@PathVariable("page") final String page,
			// @PathVariable("sub") final String subFolder,
			final HttpServletResponse response) throws IOException {

		final File physicalFile = new File("/home/glodt/data/" + page + "/" + fileName);

		// response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate().getHeaderValue());
		response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=0, private"); // CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate().getHeaderValue());
		response.setDateHeader(HttpHeaders.LAST_MODIFIED, physicalFile.lastModified());
		response.setHeader(HttpHeaders.EXPIRES, "0");
		response.setHeader(HttpHeaders.ETAG, fileName + "-" + (physicalFile.lastModified() / 1000));

		return new FileSystemResource(physicalFile);
	}

	@ResponseBody
	@GetMapping(value = "/photo/s/{page}/{size}/{filename:.*}")
	public ResponseEntity<InputStreamResource> getPicThumbnail(
			@PathVariable("filename") final String fileName,
			@PathVariable("page") final String page,
			// @PathVariable("sub") final String subFolder,
			@PathVariable("size") final int size,
			final HttpServletResponse response) throws IOException {

		response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=0, private"); // CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate().getHeaderValue());
		// response.setHeader("Expires", "0");

		final String absoluteFileName = "/home/glodt/data/" + page + "/" + fileName;
		final String fileNameBase = StringUtils.substringBeforeLast(fileName, ".");
		final String fileNameExt = StringUtils.substringAfterLast(fileName, ".");

		final File physicalThumbnailFolder = new File("/home/glodt/data/" + page + "/" + "/thumbnails");
		if (!physicalThumbnailFolder.exists()) {
			physicalThumbnailFolder.mkdir();
		}

		final File physicalThumbnailFile = new File("/home/glodt/data/" + page + "/" + "/thumbnails/" + fileNameBase + "-" + size + "." + fileNameExt);
		// byte[] bytes = null;
		InputStream pic = null;

		if (physicalThumbnailFile.exists()) {

			logger.debug("Thumbnail exists         : " + physicalThumbnailFile.getCanonicalPath());

			pic = new FileInputStream(physicalThumbnailFile);

			// return ResponseEntity
			// .ok()
			// // .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS)) does not work... We need response.setHeader() above
			// .lastModified(physicalThumbnailFile.lastModified())
			// .eTag((physicalThumbnailFile.lastModified() / 1000) + "")
			// .contentLength(physicalThumbnailFile.length())
			// .contentType(MediaType.parseMediaType("image/jpg"))
			// .body(new InputStreamResource(new FileInputStream(physicalThumbnailFile)));
		} else {
			final File physicalFile = new File(absoluteFileName);

			logger.debug("Thumbnail does not exist : " + physicalThumbnailFile.getCanonicalPath());

			final byte[] resizedPic = Utils.resizeImage(FileUtils.readFileToByteArray(physicalFile), fileNameExt, size);
			FileUtils.writeByteArrayToFile(physicalThumbnailFile, resizedPic);

			pic = new ByteArrayInputStream(resizedPic);

			// return ResponseEntity
			// .ok()
			// // .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS)) does not work... We need response.setHeader() above
			// .lastModified(physicalThumbnailFile.lastModified())
			// .eTag((physicalThumbnailFile.lastModified() / 1000) + "")
			// .contentLength(resizedPic.length)
			// .contentType(MediaType.parseMediaType("image/jpg"))
			// .body(new InputStreamResource(new ByteArrayInputStream(resizedPic)));
		}

		return ResponseEntity
				.ok()
				// .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS)) does not work... We need response.setHeader() above
				.lastModified(physicalThumbnailFile.lastModified())
				.eTag((physicalThumbnailFile.lastModified() / 1000) + "")
				.contentLength(physicalThumbnailFile.length())
				.contentType(MediaType.parseMediaType("image/jpg"))
				.body(new InputStreamResource(pic));
	}

}
