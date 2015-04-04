<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="row">
	<div class="col-md-9">
		<h4>Texter aus dem ${title} ${name}</h4>
		<div class="textcolumns">
			<%@ include file="_pagesInfos.jsp"%>
		</div>
		<hr />
	</div>
	<div class="col-md-3">
		<%@ include file="_districts.jsp"%>
		<%@ include file="_cantons.jsp"%>
	</div>
</div>

<%@ include file="_footer.jsp"%>
