package lu.tessyglodt.site.spring;

import lu.tessyglodt.site.service.PageService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

	private static Logger	logger	= Logger.getLogger(Scheduler.class);

	@Autowired
	private PageService		pageService;

	@Value("${spring.datasource.driverClassName}")
	private String			driverClassName;

	@Value("${spring.datasource.url}")
	private String			datasourceUrl;

	@Value("${spring.datasource.username}")
	private String			datasourceUsername;

	@Value("${spring.datasource.password}")
	private String			datasourcePassword;

	@Value("${app.database.backupPath}")
	private String			databaseBackupPath;

	// @Scheduled(cron = "0 * * * * ?")
	// @Scheduled(cron = "0 0 1 * * ?")
	public void backupH2Database() {
		/*
		 * new File(databaseBackupPath).mkdirs();
		 * 
		 * final String backupName = (StringUtils.isEmpty(databaseBackupPath) ?
		 * "/tmp" : databaseBackupPath) + "/backup-db-tessyglodt_lu-" + new
		 * SimpleDateFormat("dd").format(new Date()) + ".sql";
		 * 
		 * logger.debug("Backing up database to file " + backupName);
		 * 
		 * try { Script.process(datasourceUrl, datasourceUsername,
		 * datasourcePassword, backupName, "", ""); } catch (final SQLException
		 * e) { logger.error(e.getMessage()); }
		 * 
		 * logger.debug("Compressing database backup");
		 * 
		 * try { final String[] commande = { "bzip2", "--best", backupName };
		 * final Process p = Runtime.getRuntime().exec(commande); p.waitFor(); }
		 * catch (final IOException | InterruptedException e) {
		 * logger.error(e.getMessage()); }
		 */
	}

}
