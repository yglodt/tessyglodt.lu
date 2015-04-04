<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="slg" uri="http://github.com/slugify"%>

<h4>Lëscht no Distrikt</h4>
<ul>
	<c:forEach items="${districts}" var="district">
		<li><a href="<c:url value='/district' />/${slg:slugify(district.name)}">${district.name}</a></li>
	</c:forEach>
</ul>

