package lu.tessyglodt.site.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class OrderMapper implements RowMapper<Order> {

	@Override
	public Order mapRow(final ResultSet rs, final int rowNum) throws SQLException {

		final Order o = new Order(rs.getInt("id"));

		o.setTitle(rs.getString("title"));
		o.setFirstName(rs.getString("first_name"));
		o.setLastName(rs.getString("last_name"));

		o.setHouseNumber(rs.getString("house_number"));
		o.setStreet(rs.getString("street"));
		o.setZipCode(rs.getString("zip_code"));
		o.setCity(rs.getString("city"));
		o.setCountry(rs.getString("country"));
		o.setEmail(rs.getString("email"));

		o.setOrderDate(rs.getTimestamp("order_date"));
		o.setOrderCopies(rs.getShort("order_copies"));
		o.setOrderAmount(rs.getBigDecimal("order_amount"));

		return o;
	}
}
