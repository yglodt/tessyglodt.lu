package lu.tessyglodt.site.data;

public class Municipality {

	private Integer	id;
	private String	name;
	private Canton	canton;

	public Municipality() {
	}

	public Municipality(final Integer id) {
		this.id = id;
	}

	public Municipality(final Integer id, final String name) {
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

	public String getFullName() {
		return name + " / " + getCanton().getName() + " / " + getCanton().getDistrict().getName();
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Canton getCanton() {
		return canton;
	}

	public void setCanton(final Canton canton) {
		this.canton = canton;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

}
