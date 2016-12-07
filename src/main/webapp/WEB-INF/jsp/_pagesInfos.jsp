<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<ul>
	<c:forEach items="${pages}" var="page">
		<li><i class="material-icons">place</i><a href="<c:url value='/page' />/${page.name}">${page.title}</a></li>
	</c:forEach>
</ul>
