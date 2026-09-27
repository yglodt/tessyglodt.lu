package lu.tessyglodt.site.spring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.service.PageService;
import twitter4j.TwitterException;

@Component
public class Scheduler {

	private final PageService pageService;

	public Scheduler(final PageService pageService) {
		this.pageService = pageService;
	}

	@Scheduled(cron = "0 15 8 * * ?", zone = "Europe/Luxembourg")
	public void tweet() throws TwitterException {
		// final Page page = pageService.getRandomPage();
		// pageService.tweetPage(page);
	}

}
