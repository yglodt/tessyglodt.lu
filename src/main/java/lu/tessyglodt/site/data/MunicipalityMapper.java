package lu.tessyglodt.site.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class MunicipalityMapper implements RowMapper<Municipality> {

	@Override
	public Municipality mapRow(final ResultSet rs, final int rowNum) throws SQLException {

		final Municipality municipality = new Municipality();
		municipality.setId(rs.getInt("id"));
		municipality.setName(rs.getString("name"));
		municipality.setCanton(new Canton(rs.getInt("can_id"), rs.getString("can_name")));
		municipality.getCanton().setDistrict(new District(rs.getInt("dist_id"), rs.getString("dist_name")));

		return municipality;
	}
}
