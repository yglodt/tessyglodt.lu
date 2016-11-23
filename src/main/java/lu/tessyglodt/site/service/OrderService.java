package lu.tessyglodt.site.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import lu.tessyglodt.site.data.Order;
import lu.tessyglodt.site.data.OrderMapper;

@Component
public class OrderService {

	final static Logger		logger	= LoggerFactory.getLogger(OrderService.class);

	@Autowired
	private JdbcTemplate	jdbcTemplate;

	@Autowired
	private JavaMailSender	mailSender;

	public Long countOrders() {
		return jdbcTemplate.queryForObject("select count(id) from orders", Long.class);
	}

	public List<Order> getOrders() {
		final String sql = "select "
				+ "id, title, last_name, first_name, house_number, street, "
				+ "zip_code, city, country, email, order_copies, order_amount, order_date "
				+ "from orders order by order_date desc";
		final List<Order> rows = jdbcTemplate.query(sql, new Object[] {}, new OrderMapper());
		return rows;
	}

	public List<Map<String, Object>> getParameters() {
		return jdbcTemplate.queryForList("select * from parameters order by id");
	}

	public void insert(final Order order) {

		final BigDecimal price = jdbcTemplate.queryForObject("select num_value from parameters where id = 'blizzy_price'", BigDecimal.class);

		order.setOrderAmount(price.multiply(new BigDecimal(order.getOrderCopies())));

		final String sql = "insert into orders "
				+ "(title, last_name, first_name, house_number, street, "
				+ "zip_code, city, country, email, order_copies, order_amount) "
				+ "values "
				+ "(?,?,?,?,?,"
				+ "?,?,?,?,?,?)";

		/*
		 * List<Map<String, Object>> keyList = new LinkedList<>(); Map<String,
		 * Object> key = new HashMap<>(); key.put("id", Integer.class);
		 * keyList.add(key); KeyHolder holder = new GeneratedKeyHolder(keyList);
		 */

		final KeyHolder holder = new GeneratedKeyHolder();

		// jdbcTemplate.update(sql, order.getTitle(), order.getLastName(),
		// order.getFirstName(), order.getHouseNumber(), order.getStreet(),
		// order.getZipCode(), order.getCity(), order.getCountry(),
		// order.getEmail(), order.getOrderCopies(), order.getOrderAmount());

		jdbcTemplate.update((PreparedStatementCreator) connection -> {
			final PreparedStatement ps = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, order.getTitle());
			ps.setString(2, order.getLastName());
			ps.setString(3, order.getFirstName());
			ps.setString(4, order.getHouseNumber());
			ps.setString(5, order.getStreet());
			ps.setString(6, order.getZipCode());
			ps.setString(7, order.getCity());
			ps.setString(8, order.getCountry());
			ps.setString(9, order.getEmail());
			ps.setShort(10, order.getOrderCopies());
			ps.setBigDecimal(11, order.getOrderAmount());
			return ps;
		}, holder);

		final Integer orderId = (Integer) holder.getKeys().get("id");

		order.setId(orderId);
	}

	public void sendConfirmationEmail(final Order order) throws MessagingException, UnsupportedEncodingException {

		final Map<String, String> titleMap = new HashMap<>();
		titleMap.put("MS", "Madame");
		titleMap.put("MR", "Monsieur");

		final Map<String, String> countryMap = new HashMap<>();
		countryMap.put("lu", "Lëtzebuerg / Luxembourg");
		countryMap.put("be", "Belsch / Belgique");
		countryMap.put("de", "Däitschland / Allemagne");
		countryMap.put("fr", "Frankräich / France");

		final MimeMessage message = mailSender.createMimeMessage();
		message.setSubject("Bestellung / Commande");
		message.setSentDate(new Date());

		final MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

		helper.setTo(new InternetAddress(order.getEmail(), order.getLastName() + " " + order.getFirstName()));
		// helper.setFrom(new InternetAddress("site@tessyglodt.lu", "Blizzy"));
		helper.setFrom(new InternetAddress("tessy.glodt@gmx.net", "Tessy GLODT-RAUS"));
		helper.addBcc("tanyv@mind.lu");
		helper.addBcc("tessy.glodt@gmx.net");
		// helper.setReplyTo(replyTo, personal);

		final StringBuilder body = new StringBuilder();
		body.append("Merci fir Är Bestellung. Nodeems d'Iwwerweisung op eisem Kont ukomm ass kritt dir d'Bestellung zougescheckt.\n\n");
		body.append("Merci pour votre commande. Après comptabilisation du paiement sur notre compte votre commande sera expédiée.\n\n");

		body.append("Bankverbindung / Coordonnées bancaires:\n\n");
		body.append("Kont / Compte: LU28 0019 4006 4475 0000, CCPLULL.\n");
		body.append("Titulaire: Tessy GLODT-RAUS\n");
		body.append("Mentioun / Référence: Commande Blizzy N°" + order.getId() + "\n");
		body.append("Montant: " + order.getOrderAmount() + " EUR\n\n");

		body.append("Titel / Titre: " + titleMap.get(order.getTitle()) + "\n");
		body.append("Nonumm / Nom de famille: " + order.getLastName() + "\n");
		body.append("Virnumm / Prénom: " + order.getFirstName() + "\n");
		body.append("Email: " + order.getEmail() + "\n");
		body.append("Hausnummer / Numéro: " + order.getHouseNumber() + "\n");
		body.append("Strooss / Rue: " + order.getStreet() + "\n");
		body.append("Postleitzuel / Code postal: " + order.getZipCode() + "\n");
		body.append("Uertschaft / Ville: " + order.getCity() + "\n");
		body.append("Land / Pays: " + countryMap.get(order.getCountry()) + "\n");
		body.append("Exemplairen / Exemplaires: " + order.getOrderCopies() + "\n");

		message.setText(body.toString());
		// helper.addInline("header", new ClassPathResource("img/mylogo.gif"));

		mailSender.send(message);
	}

}
