<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="row">

	<div class="col-md-9">
		<div class="row">
			<div class="col-sm-6">
				<%@ include file="_randomPage.jsp"%>
			</div>
			<div class="col-sm-6">
				<h4>Kaart</h4> <a href="<c:url value='/kaart' />"><img id="index_mapimage" src="<c:url value='/resources/img/kaart.jpg' />" width="300" height="164"></a> <br /> <br />
			</div>
		</div>

		<div class="row">
			<div class="col-xs-12">
				<h4>All d'Texter</h4>
				<div class="textcolumns">
					<%@ include file="_pagesInfos.jsp"%>
				</div>
			</div>
		</div>
	</div>

	<div class="col-md-3">

		<%@ include file="_newestPages.jsp"%>

		<%@ include file="_lastReadPages.jsp"%>

		<%@ include file="_mostReadPages.jsp"%>

		<%@ include file="_districts.jsp"%>

		<%@ include file="_cantons.jsp"%>

	</div>

</div>

<%@ include file="_footer.jsp"%>
