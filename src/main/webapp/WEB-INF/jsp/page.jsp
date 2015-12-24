<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>

<%@ include file="_header.jsp"%>

<script type="text/javascript" src="https://ssl.panoramio.com/wapi/wapi.js?v=1"></script>

<div class="row">

	<div class="col-md-9 text">
		<h3>${page.title}</h3> ${page.content}
	</div>

	<div class="col-md-3">

		<sec:authorize access="isAuthenticated()">
			<p><a class="btn btn-warning" href="<c:url value='/admin/pageform' />?id=${page.id}">Änneren</a></p>
		</sec:authorize>

		<h4>Informatiounen zu ${page.title}</h4>

		<table class="table">

			<tbody>
				<tr>
					<th>Gemeng</th>
					<td>${page.municipality.name}</td>
				</tr>
				<tr>
					<th>Kanton</th>
					<td>${page.municipality.canton.name}</td>
				</tr>
				<tr>
					<th>Distrikt</th>
					<td>${page.municipality.canton.district.name}</td>
				</tr>
				<tr>
					<th>Publizéiert den</th>
					<td><fmt:formatDate pattern="dd.MM.yyyy" value="${page.datePublished}" /></td>
				</tr>
			</tbody>
		</table>

		<h4>Op der Landkaart</h4>
		<div id="map"></div>

		<h4>Fotoen aus der Ëmgéigend</h4>
		<div id="photos"></div>

	</div>

</div>

<script type="text/javascript">
	function loadScript() {
		var script = document.createElement("script");
		script.type = "text/javascript";
		script.src = "//maps.googleapis.com/maps/api/js?key=AIzaSyCJneX2_ynuWwRZthXVC_sf4AC5j4V5nqA&sensor=false&callback=showMap";
		document.body.appendChild(script);
	}

	function showMap() {

		var myLatlng = new google.maps.LatLng("${page.latitude}",
				"${page.longitude}");

		// var center = new google.maps.LatLng(49.6, 6.116667);
		var center = new google.maps.LatLng(49.85, 6.1);

		var myOptions = {
			zoom : 8,
			center : center,
			mapTypeId : google.maps.MapTypeId.ROADMAP
		}

		var map = new google.maps.Map(document.getElementById("map"), myOptions);

		var marker = new google.maps.Marker({
			position : myLatlng,
			title : "${page.title}"
		});

		// To add the marker to the map, call setMap();
		marker.setMap(map);

		showPhotos();
	}
<%-- http://gis.stackexchange.com/questions/33238/how-do-you-get-the-coordinates-from-a-click-or-drag-event-in-the-google-maps-api --%>
	function showPhotos() {
<%-- replaced all instances of 1.0/200 with 1.0/100 on Nov 25 2013 to get more pics --%>
	var myRequest = {
			'rect' : {
				'sw' : {
					'lat' : "<fmt:formatNumber maxIntegerDigits='2' value='${page.latitude - 0.01}' maxFractionDigits='6' />",
					'lng' : "<fmt:formatNumber maxIntegerDigits='2' value='${page.longitude - 0.01}' maxFractionDigits='6' />"
				},
				'ne' : {
					'lat' : "<fmt:formatNumber maxIntegerDigits='2' value='${page.latitude + 0.01}' maxFractionDigits='6' />",
					'lng' : "<fmt:formatNumber maxIntegerDigits='2' value='${page.longitude + 0.01}' maxFractionDigits='6' />"
				}
			}
		};

		var myOptions = {
			'width' : '100%',
			'height' : 200,
			croppedPhotos : false
		};

		var widget = new panoramio.PhotoWidget('photos', myRequest, myOptions);
		widget.enableNextArrow(false);
		widget.enablePreviousArrow(false);

		widget.setPosition(0);

		//console.log("1end: ", widget.getAtEnd(), "1start: ", widget.getAtStart());

		i = 1;
		timeout = 5000;

		function nextPic() {
			//console.log("2end: ", widget.getAtEnd(), "2start: ", widget.getAtStart());
			if (widget.getAtEnd() == true)
				i = 0;
			widget.setPosition(i);
			i++;
			window.setTimeout(nextPic, timeout);
		}

		window.setTimeout(nextPic, timeout);

	}

	window.onload = loadScript;
</script>

<%@ include file="_footer.jsp"%>
