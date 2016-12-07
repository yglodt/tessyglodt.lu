<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="box margin-top margin-bottom">
	<article class="text-box">
		<h4>Statistiken</h4>
		<table id="stats_table" class="table table-condensed table-hover">
			<tr>
				<th>Säit</th>
				<th>Unzuel Klicks</th>
				<th>Läschte Klick</th>
			</tr>
			<c:forEach items="${pages}" var="page">
				<tr>
					<td>${page.title}</td>
					<td>${page.view_count}</td>
					<td><javatime:format pattern="dd.MM.yyyy - HH:mm" value="${page.date_last_view.toLocalDateTime()}" /></td>
				</tr>
			</c:forEach>
		</table>
	</article>
</div>

<%@ include file="_footer.jsp"%>
