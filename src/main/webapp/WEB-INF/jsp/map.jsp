<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<h4>Kaart mat den Uertschaften</h4>

<div id="bigmap" style="width: 100%; height: 600px;"></div>

<script type="text/javascript">
	function loadScript() {
		var script = document.createElement("script");
		script.type = "text/javascript";
		script.src = "http://maps.googleapis.com/maps/api/js?key=AIzaSyCJneX2_ynuWwRZthXVC_sf4AC5j4V5nqA&sensor=false&callback=showMap";
		document.body.appendChild(script);
	}

	function showMap() {

		// http://code.google.com/intl/lb/apis/maps/documentation/javascript/reference.html

		var center = new google.maps.LatLng(49.85, 6.1);

		var myOptions = {
			zoom : 9,
			center : center,
			mapTypeId : google.maps.MapTypeId.ROADMAP,
			draggable : true,
			scrollwheel : false,
			disableDoubleClickZoom : true
		};

		var map = new google.maps.Map(document.getElementById("bigmap"), myOptions);

		//<c:forEach items="${pageInfos}" var="page"><c:if test="${not empty page.latitude and not empty page.longitude}">
		var myLatlng = new google.maps.LatLng("${page.latitude}","${page.longitude}"); var marker = new google.maps.Marker({position:myLatlng,title:"${page.title}"});
		google.maps.event.addListener(marker, 'click', function() {window.location.href = "<c:url value='/page/' />${page.name}";}); marker.setMap(map);
		//</c:if></c:forEach>

	}

	window.onload = loadScript;
</script>

<%@ include file="_footer.jsp"%>
