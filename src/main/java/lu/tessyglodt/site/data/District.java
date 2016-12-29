package lu.tessyglodt.site.data;

public class District {

	private Integer	id;
	private String	name;
	private String	slug;

	public District() {
	}

	public District(final Integer id) {
		this.id = id;
	}

	public District(final Integer id, final String name) {
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
