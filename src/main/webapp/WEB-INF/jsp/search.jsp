<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="cont main">
	<article>
		<h4>Resultat vun der Sich no '${param.q}'</h4>
		<c:choose>
			<c:when test="${empty pages}">
				<p>Keng Säite fonnt.</p>
			</c:when>
			<c:otherwise>
				<ul>
					<c:forEach items="${pages}" var="page">
						<li><a href="<c:url value='/page' />/${page.name}">${page.title}</a></li>
					</c:forEach>
				</ul>
			</c:otherwise>
		</c:choose>
	</article>
</div>

<%@ include file="_footer.jsp"%>
