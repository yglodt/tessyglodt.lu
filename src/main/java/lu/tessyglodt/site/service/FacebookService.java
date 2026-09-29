package lu.tessyglodt.site.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

import lu.tessyglodt.site.data.Page;

// Posts a random page to the Facebook Page once a day via the Graph API
@Component
public class FacebookService {

	final static Logger			logger	= LoggerFactory.getLogger(FacebookService.class);

	private final PageService	pageService;

	private final RestClient	restClient;

	private final JsonMapper	jsonMapper	= JsonMapper.builder().build();

	private final boolean		enabled;

	private final String		pageId;

	private final String		accessToken;

	public FacebookService(final PageService pageService,
			@Value("${facebook.enabled:false}") final boolean enabled,
			@Value("${facebook.page-id:}") final String pageId,
			@Value("${facebook.access-token:}") final String accessToken,
			@Value("${facebook.api-version:v26.0}") final String apiVersion) {
		this.pageService = pageService;
		this.enabled = enabled;
		this.pageId = pageId;
		this.accessToken = accessToken;
		this.restClient = RestClient.create("https://graph.facebook.com/" + apiVersion);
	}

	@Scheduled(cron = "${facebook.cron:0 15 7 * * *}", zone = "Europe/Luxembourg")
	public void postRandomPageScheduled() {
		if (!enabled) {
			return;
		}
		try {
			postRandomPage();
		} catch (final Exception e) {
			logger.error("Posting to Facebook failed", e);
		}
	}

	// Returns a short description of what was posted
	public String postRandomPage() {
		if (ObjectUtils.isEmpty(pageId) || ObjectUtils.isEmpty(accessToken)) {
			throw new IllegalStateException("facebook.page-id and facebook.access-token must be set");
		}

		final Page page = pageService.getRandomPage();

		final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("message", getMessage(page));
		form.add("link", page.getUrl());
		form.add("access_token", accessToken);

		// The Graph API answers with content type text/javascript, which no converter maps to JSON
		final String response = restClient.post()
				.uri("/{pageId}/feed", pageId)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(String.class);

		final String result = "Posted " + page.getUrl() + " as Facebook post " + jsonMapper.readTree(response).path("id").asString();
		logger.info(result);
		return result;
	}

	private static String getMessage(final Page page) {
		final StringBuilder sb = new StringBuilder(page.getTitle());
		if (page.getMunicipality() != null) {
			sb.append("\n\uD83D\uDCCD Gemeng ").append(page.getMunicipality().getName());
			if (page.getMunicipality().getCanton() != null) {
				sb.append(", Kanton ").append(page.getMunicipality().getCanton().getName());
			}
		}
		sb.append("\n\n").append(page.getTeaser());
		// Facebook has no links behind text, so the URL itself is the link
		sb.append("\n\nWeiderliesen op tessyglodt.lu: ").append(page.getUrl());
		return sb.toString();
	}

}
