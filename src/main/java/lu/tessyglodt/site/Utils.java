package lu.tessyglodt.site;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.web.util.HtmlUtils;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;

import lu.tessyglodt.site.data.Page;

public class Utils {

	public static Integer getValue(final Object o) {
		if (o == null) {
			return null;
		} else {
			return Integer.valueOf(o.toString());
		}
	}

	/*
	 * public static File stream2file(final InputStream in) throws IOException {
	 * final File tempFile = File.createTempFile("stream2file", ".tmp");
	 * tempFile.deleteOnExit(); try (FileOutputStream out = new
	 * FileOutputStream(tempFile)) { IOUtils.copy(in, out); } return tempFile; }
	 */

	public static SyndFeed createFeed(final String name, final List<Page> pages) {

		final SyndFeed feed = new SyndFeedImpl();
		/*
		 * rss_0.9, rss_0.91, rss_0.92, rss_0.93, rss_0.94, rss_1.0, rss_2.0,
		 * atom_0.3, or atom_1.0
		 */
		feed.setFeedType("atom_1.0");
		// feed.setFeedType("rss_2.0");

		feed.setTitle("Kierchtuerms­promenaden");
		feed.setDescription(name);
		feed.setLink("https://www.tessyglodt.lu/");
		feed.setAuthor("Tessy Glodt");

		final List<SyndEntry> entries = new ArrayList<>();

		for (final Page page : pages) {

			final SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(page.getTitle());
			entry.setLink(page.getUrl());
			entry.setPublishedDate(Date.from(page.getDatePublished().atStartOfDay(ZoneId.systemDefault()).toInstant()));
			entry.setAuthor("Tessy Glodt");

			final SyndContent content = new SyndContentImpl();
			// content.setType("text/plain");
			content.setValue(HtmlUtils.htmlUnescape(page.getContent()));

			// entry.setContents(Collections.singletonList(content));
			entries.add(entry);
		}

		feed.setEntries(entries);

		return feed;
	}
}
