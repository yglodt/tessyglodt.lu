<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="cont main">
	<article>
		<h4>Kaart mat den Uertschaften</h4>
		<div id="bigmap" style="width: 100%; height: 900px;"></div>
	</article>
</div>

<script type="text/javascript">
	function loadScript() {
		var script = document.createElement("script");
		script.type = "text/javascript";
		script.src = "//maps.googleapis.com/maps/api/js?key=AIzaSyCJneX2_ynuWwRZthXVC_sf4AC5j4V5nqA&sensor=false&callback=showMap";
		document.body.appendChild(script);
	}

	function showMap() {

		// http://code.google.com/intl/lb/apis/maps/documentation/javascript/reference.html

		var center = new google.maps.LatLng(49.85, 6.1);

		var myOptions = {
			zoom : 10,
			center : center,
			mapTypeId : google.maps.MapTypeId.ROADMAP,
			draggable : true,
			scrollwheel : true,
			disableDoubleClickZoom : true
		};

		var n = new google.maps.Map(document.getElementById("bigmap"),
				myOptions);

		//<c:forEach items="${pageInfos}" var="page"><c:if test="${not empty page.latitude and not empty page.longitude}">
		var l = new google.maps.LatLng(
				"<fmt:formatNumber maxIntegerDigits='2' value='${page.latitude}' maxFractionDigits='6' />",
				"<fmt:formatNumber maxIntegerDigits='2' value='${page.longitude}' maxFractionDigits='6' />");
		var m = new google.maps.Marker({
			position : l,
			title : "${page.title}"
		});
		google.maps.event.addListener(m, 'click', function() {
			window.location.href = "<c:url value='/page/' />${page.name}";
		});
		m.setMap(n);
		//</c:if></c:forEach>

	}

	window.onload = loadScript;
</script>

<%@ include file="_footer.jsp"%>
