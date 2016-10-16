<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="cont main">
	<aside>
		<%@ include file="_districts.jsp"%>
		<%@ include file="_cantons.jsp"%>
	</aside>
	<article>
		<h4>Texter aus dem ${title} ${name}</h4>
		<div class="textcolumns">
			<%@ include file="_pagesInfos.jsp"%>
		</div>
	</article>
</div>

<%@ include file="_footer.jsp"%>
