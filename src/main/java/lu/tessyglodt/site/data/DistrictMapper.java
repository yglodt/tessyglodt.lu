package lu.tessyglodt.site.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DistrictMapper implements RowMapper<District> {

	@Override
	public District mapRow(final ResultSet rs, final int rowNum) throws SQLException {

		final District district = new District();
		district.setId(rs.getInt("id"));
		district.setName(rs.getString("name"));

		return district;
	}
}
