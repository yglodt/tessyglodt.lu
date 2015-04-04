package lu.tessyglodt.site.data;

import java.io.IOException;

import com.github.slugify.Slugify;

public class Canton {

	private Integer		id;
	private String		name;
	private District	district;

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

	public String getSlugName() throws IOException {
		return new Slugify().slugify(name);
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

}
