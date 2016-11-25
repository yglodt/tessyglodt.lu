package lu.tessyglodt.site.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {

	private int				id;

	private String			title;
	private String			lastName;
	private String			firstName;
	private String			houseNumber;
	private String			street;
	private String			zipCode;
	private String			city;
	private String			country;
	private String			email;
	private LocalDateTime	orderDate;
	private short			orderCopies;
	private BigDecimal		orderAmount;

	public Order() {
	}

	public Order(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getHouseNumber() {
		return houseNumber;
	}

	public void setHouseNumber(String houseNumber) {
		this.houseNumber = houseNumber;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}

	public short getOrderCopies() {
		return orderCopies;
	}

	public void setOrderCopies(short orderCopies) {
		this.orderCopies = orderCopies;
	}

	public BigDecimal getOrderAmount() {
		return orderAmount;
	}

	public void setOrderAmount(BigDecimal orderAmount) {
		this.orderAmount = orderAmount;
	}

	@Override
	public String toString() {
		return "Order [id=" + id + ", " + (title != null ? "title=" + title + ", " : "") + (lastName != null ? "lastName=" + lastName + ", " : "") + (firstName != null ? "firstName=" + firstName + ", " : "") + (houseNumber != null ? "houseNumber=" + houseNumber + ", " : "") + (street != null ? "street=" + street + ", " : "") + (zipCode != null ? "zipCode=" + zipCode + ", " : "") + (city != null ? "city=" + city + ", " : "") + (country != null ? "country=" + country + ", " : "") + (email != null ? "email=" + email + ", " : "") + (orderDate != null ? "orderDate=" + orderDate + ", " : "") + "orderCopies=" + orderCopies + ", " + (orderAmount != null ? "orderAmount=" + orderAmount : "") + "]";
	}

}
