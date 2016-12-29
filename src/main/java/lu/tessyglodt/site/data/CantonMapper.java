package lu.tessyglodt.site.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class CantonMapper implements RowMapper<Canton> {

	@Override
	public Canton mapRow(final ResultSet rs, final int rowNum) throws SQLException {

		final Canton canton = new Canton();
		canton.setId(rs.getInt("id"));
		canton.setName(rs.getString("name"));
		canton.setDistrict(new District(rs.getInt("dist_id"), rs.getString("dist_name")));
		canton.setSlug(rs.getString("slug"));

		return canton;
	}
}
