<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="../_header.jsp"%>

<div class="row">
	<div class="col-md-6">
		<h4>Gemengen</h4>
		<div style="column-count: 4; -webkit-column-count: 4; -moz-column-count: 4;">
			<ul>
				<c:forEach items="${municipalities}" var="municipality">
					<li><a href="<c:url value='/admin/municipalityform' />?id=${municipality.id}">${municipality.name}</a></li>
				</c:forEach>
			</ul>
		</div>
	</div>
	<div class="col-md-2">
		<h4>Kantonen</h4>
		<ul>
			<c:forEach items="${cantons}" var="canton">
				<li><a href="<c:url value='/admin/cantonform' />?id=${canton.id}">${canton.name}</a></li>
			</c:forEach>
		</ul>
	</div>
	<div class="col-md-2">
		<h4>Distrikter</h4>
		<ul>
			<c:forEach items="${districts}" var="district">
				<li><a href="<c:url value='/admin/districtform' />?id=${district.id}">${district.name}</a></li>
			</c:forEach>
		</ul>
	</div>
	<div class="col-md-2">
		<h4>Aktiounen</h4>
		<ul>
			<li><a href="<c:url value='/admin/municipalityform' />">Nei Gemeng</a></li>
			<li><a href="<c:url value='/admin/cantonform' />">Neie Kanton</a></li>
			<li><a href="<c:url value='/admin/districtform' />">Neien Distrikt</a></li>
		</ul>
	</div>
</div>

<%@ include file="../_footer.jsp"%>
