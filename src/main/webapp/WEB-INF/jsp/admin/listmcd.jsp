<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="../_header.jsp"%>

<div class="box margin-top margin-bottom">
	<article>
		<div class="text-box">
			<h4>Gemengen</h4>
			<div style="column-count: 4; -webkit-column-count: 4; -moz-column-count: 4;">
				<ul>
					<c:forEach items="${municipalities}" var="municipality">
						<li><i class="material-icons">place</i><a href="<c:url value='/admin/municipalityform' />?id=${municipality.id}">${municipality.name}</a></li>
					</c:forEach>
				</ul>
			</div>
		</div>
		<div>
			<h4>Kantonen</h4>
			<ul>
				<c:forEach items="${cantons}" var="canton">
					<li><i class="material-icons">place</i><a href="<c:url value='/admin/cantonform' />?id=${canton.id}">${canton.name}</a></li>
				</c:forEach>
			</ul>
		</div>
		<div>
			<h4>Distrikter</h4>
			<ul>
				<c:forEach items="${districts}" var="district">
					<li><i class="material-icons">place</i><a href="<c:url value='/admin/districtform' />?id=${district.id}">${district.name}</a></li>
				</c:forEach>
			</ul>
		</div>
		<div>
			<h4>Aktiounen</h4>
			<ul>
				<li><a href="<c:url value='/admin/municipalityform' />">Nei Gemeng</a></li>
				<li><a href="<c:url value='/admin/cantonform' />">Neie Kanton</a></li>
				<li><a href="<c:url value='/admin/districtform' />">Neien Distrikt</a></li>
			</ul>
		</div>

	</article>
</div>

<%@ include file="../_footer.jsp"%>
