package lu.tessyglodt.site.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class PageMapper implements RowMapper<Page> {

	@Override
	public Page mapRow(final ResultSet rs, final int rowNum) throws SQLException {

		final Page page = new Page();
		page.setId(rs.getString("id"));
		page.setName(rs.getString("name"));
		page.setTitle(rs.getString("title"));
		page.setContent(rs.getString("content"));

		page.setLatitude(rs.getBigDecimal("latitude"));
		page.setLongitude(rs.getBigDecimal("longitude"));

		page.setDatePublished(rs.getTimestamp("date_published").toLocalDateTime());
		page.setPublished(rs.getBoolean("published"));

		page.setMunicipality(new Municipality(rs.getInt("mun_id"), rs.getString("mun_name")));
		page.getMunicipality().setCanton(new Canton(rs.getInt("can_id"), rs.getString("can_name")));
		page.getMunicipality().getCanton().setDistrict(new District(rs.getInt("dist_id"), rs.getString("dist_name")));

		page.setSite(rs.getInt("site"));
		page.setType(rs.getInt("type"));

		return page;
	}
}
