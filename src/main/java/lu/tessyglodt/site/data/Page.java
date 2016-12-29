package lu.tessyglodt.site.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public class Page {
	/*
	 * id | integer | not null default nextval('systemevents_id_seq'::regclass)
	 * customerid | bigint | receivedat | timestamp without time zone |
	 * devicereportedtime | timestamp without time zone | facility | smallint |
	 * priority | smallint | fromhost | character varying(60) | message | text |
	 * ntseverity | integer | importance | integer | eventsource | character
	 * varying(60) | eventuser | character varying(60) | eventcategory | integer
	 * | eventid | integer | eventbinarydata | text | maxavailable | integer |
	 * currusage | integer | minusage | integer | maxusage | integer |
	 * infounitid | integer | syslogtag | character varying(60) | eventlogtype |
	 * character varying(60) | genericfilename | character varying(60) |
	 * systemid | integer |
	 */

	// private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	public static final int	TYPE_KIERCHTUERMS­PROMENADEN_ARTICLE	= 0;
	public static final int	TYPE_KIERCHTUERMS­PROMENADEN_PAGE		= 1;

	public static final int	SITE_KIERCHTUERMS­PROMENADEN			= 0;

	private String			id;

	private String			name;

	private String			title;

	private String			content;

	private Municipality	municipality;

	private BigDecimal		latitude;

	private BigDecimal		longitude;

	// private Integer/BigDecimal fotoWindow in metres ideally
	// http://stackoverflow.com/questions/7477003/calculating-new-longtitude-latitude-from-old-n-meters

	private Integer			viewCount;

	private boolean			published								= true;

	private LocalDateTime	dateCreated;

	private LocalDateTime	dateModified;

	@DateTimeFormat(iso = ISO.DATE)
	private LocalDate		datePublished;

	private LocalDateTime	dateLastView;

	private Integer			site									= SITE_KIERCHTUERMS­PROMENADEN;
	private Integer			type									= TYPE_KIERCHTUERMS­PROMENADEN_ARTICLE;

	public Page() {
	}

	public Page(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public Municipality getMunicipality() {
		return municipality;
	}

	public void setMunicipality(final Municipality municipality) {
		this.municipality = municipality;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(final BigDecimal latitude) {
		this.latitude = latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(final BigDecimal longitude) {
		this.longitude = longitude;
	}

	public LocalDateTime getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(final LocalDateTime dateCreated) {
		this.dateCreated = dateCreated;
	}

	public LocalDateTime getDateModified() {
		return dateModified;
	}

	public void setDateModified(final LocalDateTime dateModified) {
		this.dateModified = dateModified;
	}

	public LocalDate getDatePublished() {
		return datePublished;
	}

	public void setDatePublished(final LocalDate datePublished) {
		this.datePublished = datePublished;
	}

	// public void setDatePublished(final String datePublished) {
	// this.datePublished = Instant.ofEpochMilli(new
	// Long(datePublished)).atZone(ZoneId.systemDefault()).toLocalDateTime();
	// }

	public Integer getViewCount() {
		return viewCount;
	}

	public void setViewCount(final Integer viewCount) {
		this.viewCount = viewCount;
	}

	public LocalDateTime getDateLastView() {
		return dateLastView;
	}

	public void setDateLastView(final LocalDateTime dateLastView) {
		this.dateLastView = dateLastView;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(final boolean published) {
		this.published = published;
	}

	public Integer getSite() {
		return site;
	}

	public void setSite(final Integer site) {
		this.site = site;
	}

	public Integer getType() {
		return type;
	}

	public void setType(final Integer type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Page [name=" + name + ", title=" + title + ", latitude="
				+ latitude + ", longitude=" + longitude + ", datePublished="
				+ datePublished + "]";
	}

	public String getUrl() {
		return "https://www.tessyglodt.lu/page/" + name;
	}

	public String getTweet() {
		// https://support.twitter.com/articles/78124
		final Document content = Jsoup.parse(getContent());
		final String contentAsText = content.text().trim();
		final String lengthIndicator = " 12345678901234567890123 #" + getTitle() + (getTitle().toLowerCase().equals(getName().toLowerCase()) ? "" : " #" + getName()) + " #Lëtzebuerg";
		final String suffix = " " + getUrl() + " #" + getTitle() + (getTitle().toLowerCase().equals(getName().toLowerCase()) ? "" : " #" + getName()) + " #Lëtzebuerg";
		return "\"" + contentAsText.substring(0, 137 - lengthIndicator.length()) + "…\"" + suffix;
	}

}
