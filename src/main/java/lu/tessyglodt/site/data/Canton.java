package lu.tessyglodt.site.data;

public class Canton {

	private Integer		id;
	private String		name;
	private District	district;
	private String		slug;

	public Canton() {
	}

	public Canton(final Integer id) {
		this.id = id;
	}

	public Canton(final Integer id, final String name) {
		this(id);
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public District getDistrict() {
		return district;
	}

	public void setDistrict(final District district) {
		this.district = district;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

}
