package lu.tessyglodt.site.spring;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.data.Page;
import lu.tessyglodt.site.service.PageService;

@Component
public class Scheduler {

	private static Logger	logger	= Logger.getLogger(Scheduler.class);

	@Autowired
	private PageService		pageService;

	// @Scheduled(cron = "*/5 * * * * ?")
	@Scheduled(cron = "0 15 7 * * ?", zone = "Europe/Luxembourg")
	public void tweet() {
		Page page = pageService.getRandomPage();
		pageService.tweetPage(page);
	}

}
