package lu.tessyglodt.site.data;

// A page near another one, for the "An der Géigend" box
public class NearbyPage {

	private final String	name;

	private final String	title;

	// Straight-line distance in kilometres
	private final double	distance;

	public NearbyPage(final String name, final String title, final double distance) {
		this.name = name;
		this.title = title;
		this.distance = distance;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public double getDistance() {
		return distance;
	}

}
