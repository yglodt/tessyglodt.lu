<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<h4>Statistiken</h4>

<table class="table table-condensed table-hover">
	<tr>
		<th>Säit</th>
		<th>Unzuel Klicks</th>
		<th>Läschte Klick</th>
	</tr>
	<c:forEach items="${pages}" var="page">
		<tr>
			<td>${page.title}</td>
			<td>${page.view_count}</td>
			<td><fmt:formatDate value="${page.date_last_view}" pattern="dd.MM.yyyy - HH:mm" /></td>
		</tr>
	</c:forEach>
</table>

<%@ include file="_footer.jsp"%>
