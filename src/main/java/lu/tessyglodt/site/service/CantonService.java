package lu.tessyglodt.site.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Canton;
import lu.tessyglodt.site.data.CantonMapper;

@Component
public class CantonService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public Canton getCanton(final Integer id) {
		final String sql = "select "
				+ "c.id, c.name, slugify(c.name) as \"slug\", "
				+ "d.id as dist_id, d.name as dist_name "
				+ "from canton c "
				+ "left join district d on d.id = c.district "
				+ "where c.id = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { id }, new CantonMapper());
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public Canton getCantonBySlugifiedName(final String name) {
		final String sql = "select "
				+ "c.id, c.name, slugify(c.name) as \"slug\", "
				+ "d.id as dist_id, d.name as dist_name "
				+ "from canton c "
				+ "left join district d on d.id = c.district "
				+ "where slugify(c.name) = ?";
		return jdbcTemplate.queryForObject(sql, new Object[] { name }, new CantonMapper());
	}

	@Cacheable(value = "page", key = "#root.methodName")
	public List<Canton> getCantons() {
		final String sql = "select "
				+ "c.id, c.name, slugify(c.name) as \"slug\", "
				+ "d.id as dist_id, d.name as dist_name "
				+ "from canton c "
				+ "left join district d on d.id = c.district "
				+ "order by c.name asc";
		return jdbcTemplate.query(sql, new CantonMapper());
	}

	@CacheEvict(value = "page", allEntries = true)
	public void insert(final Canton canton) {
		final String sql = "insert into canton (name, district) values (?,?)";
		jdbcTemplate.update(sql, canton.getName(), Utils.getValue(canton.getDistrict()));
	}

	@CacheEvict(value = "page", allEntries = true)
	public void update(final Canton canton) {
		final String sql = "update canton set name = ?, district = ? where id = ?";
		jdbcTemplate.update(sql, canton.getName(), Utils.getValue(canton.getDistrict()), canton.getId());
	}

}
