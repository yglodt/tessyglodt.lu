package lu.tessyglodt.site.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.service.PageService;
import twitter4j.TwitterException;

@Component
public class Scheduler {

	@Autowired
	private PageService pageService;

	// @Scheduled(cron = "*/5 * * * * ?")
	@Scheduled(cron = "0 15 7 * * ?", zone = "Europe/Luxembourg")
	public void tweet() throws TwitterException {
		final Page page = pageService.getRandomPage();
		pageService.tweetPage(page);
	}

}
