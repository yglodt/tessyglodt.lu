<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="slg" uri="http://github.com/slugify"%>

<div class="fitem">
	<h4>Lëscht no Kanton</h4>
	<ul>
		<c:forEach items="${cantons}" var="canton">
			<li><a href="<c:url value='/canton' />/${slg:slugify(canton.name)}">${canton.name}</a></li>
		</c:forEach>
	</ul>
</div>
