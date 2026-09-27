package lu.tessyglodt.site.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.Utils;
import lu.tessyglodt.site.data.Municipality;
import lu.tessyglodt.site.data.MunicipalityMapper;

@Component
public class MunicipalityService {

	private final JdbcTemplate jdbcTemplate;

	public MunicipalityService(final JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Cacheable(value = "page", key = "#root.methodName + #p0")
	public Municipality getMunicipality(final Integer id) {
		final String sql = "select "
				+ "m.id, m.name, c.id as can_id, c.name as can_name, d.id as dist_id, d.name as dist_name "
				+ "from municipality m "
				+ "left join canton c on c.id = m.canton "
				+ "left join district d on d.id = c.district "
				+ "where m.id = ?";
		return jdbcTemplate.queryForObject(sql, new MunicipalityMapper(), id);
	}

	@Cacheable(value = "page", key = "#root.methodName")
	public List<Municipality> getMunicipalities() {
		final String sql = "select "
				+ "m.id, m.name, c.id as can_id, c.name as can_name, d.id as dist_id, d.name as dist_name "
				+ "from municipality m "
				+ "left join canton c on c.id = m.canton "
				+ "left join district d on d.id = c.district "
				+ "order by m.name asc";
		return jdbcTemplate.query(sql, new MunicipalityMapper());
	}

	@CacheEvict(value = "page", allEntries = true)
	public void insert(final Municipality municipality) {
		final String sql = "insert into municipality (name, canton) values (?, ?)";
		jdbcTemplate.update(sql, municipality.getName(), Utils.getValue(municipality.getCanton()));
	}

	@CacheEvict(value = "page", allEntries = true)
	public void update(final Municipality municipality) {
		final String sql = "update municipality set name = ?, canton = ? where id = ?";
		jdbcTemplate.update(sql, municipality.getName(), Utils.getValue(municipality.getCanton()), municipality.getId());
	}

}
