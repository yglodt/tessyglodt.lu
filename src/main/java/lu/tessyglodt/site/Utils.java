package lu.tessyglodt.site;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lu.tessyglodt.site.data.Page;

import org.springframework.web.util.HtmlUtils;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;

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

	public static int randInt(final int min, final int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		final Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		final int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	public static SyndFeed createFeed(String name, List<Page> pages) {

		SyndFeed feed = new SyndFeedImpl();
		/*
		 * rss_0.9, rss_0.91, rss_0.92, rss_0.93, rss_0.94, rss_1.0, rss_2.0,
		 * atom_0.3, or atom_1.0
		 */
		feed.setFeedType("atom_1.0");
		// feed.setFeedType("rss_2.0");

		feed.setTitle("Kierchtuerms­promenaden");
		feed.setDescription(name);
		feed.setLink("http://www.tessyglodt.lu");
		feed.setAuthor("Tessy Glodt");

		List<SyndEntry> entries = new ArrayList<>();

		for (Page page : pages) {

			SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(page.getTitle());
			entry.setLink(page.getUrl());
			entry.setPublishedDate(page.getDatePublished());
			entry.setAuthor("Tessy Glodt");

			SyndContent content = new SyndContentImpl();
			// content.setType("text/plain");
			content.setValue(HtmlUtils.htmlUnescape(page.getContent()));

			// entry.setContents(Collections.singletonList(content));
			entries.add(entry);
		}

		feed.setEntries(entries);

		return feed;
	}
}
