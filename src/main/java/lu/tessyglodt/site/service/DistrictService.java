package lu.tessyglodt.site.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.data.District;
import lu.tessyglodt.site.data.DistrictMapper;

@Component
public class DistrictService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public District getDistrict(final Integer id) {
		final String sql = "select "
				+ "d.id, d.name "
				+ "from district d "
				+ "where d.id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id }, new DistrictMapper());
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public District getDistrictBySlugifiedName(final String name) {
		final String sql = "select "
				+ "d.id, d.name "
				+ "from district d "
				+ "where slugify(d.name) = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { name }, new DistrictMapper());
	}

	@Cacheable(value = "page", key = "#root.methodName")
	public List<District> getDistricts() {
		final String sql = "select d.id, d.name from district d order by d.name asc";
		return jdbcTemplate.query(sql, new DistrictMapper());
	}

	@CacheEvict(value = "page", allEntries = true)
	public void insert(final District district) {
		final String sql = "insert into district (name) values (?)";
		jdbcTemplate.update(sql, district.getName());
	}

	@CacheEvict(value = "page", allEntries = true)
	public void update(final District district) {
		final String sql = "update district set name = ? where id = ?";
		jdbcTemplate.update(sql, district.getName(), district.getId());
	}

}
