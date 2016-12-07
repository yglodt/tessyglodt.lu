<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="../_header.jsp"%>

<link rel="stylesheet" href="<c:url value='/resources/css/bootstrap.css' />">
<link rel="stylesheet" href="<c:url value='/resources/css/bootstrap-theme.css' />">

<script type="text/javascript" src="<c:url value='/resources/js/jquery.min.js' />"></script>
<script type="text/javascript" src="<c:url value='/resources/ckeditor/ckeditor.js' />"></script>

<div class="box margin-top margin-bottom">
	<article>

		<form:form modelAttribute="page" method="post" action="${pageContext.request.contextPath}/admin/pageform" class="form-horizontal">

			<form:hidden path="id" />

			<div class="form-group">
				<form:label class="col-sm-2 control-label" path="name">Numm (fr)</form:label>
				<div class="col-sm-2">
					<form:input path="name" cssErrorClass="errors form-control" cssClass="form-control" />
				</div>
				<form:label class="col-sm-2 control-label" path="title">Titel</form:label>
				<div class="col-sm-2">
					<form:input path="title" cssErrorClass="errors form-control" cssClass="form-control" />
				</div>
			</div>

			<div class="form-group">
				<form:label class="col-sm-2 control-label" path="latitude">Latitude</form:label>
				<div class="col-sm-2">
					<form:input path="latitude" cssErrorClass="errors form-control" cssClass="form-control" type="number" step="0.0000001" min="45" max="55" />
				</div>
				<form:label class="col-sm-2 control-label" path="longitude">Longitude</form:label>
				<div class="col-sm-2">
					<form:input path="longitude" cssErrorClass="errors form-control" cssClass="form-control" type="number" step="0.0000001" min="5" max="8" />
				</div>
			</div>

			<div class="form-group">
				<form:label class="col-sm-2 control-label" path="municipality">Gemeng / Kanton / Distrikt</form:label>
				<div class="col-sm-2">
					<form:select path="municipality" cssClass="form-control" cssErrorClass="errors form-control">
						<form:option value="">[...]</form:option>
						<form:options items="${municipalities}" itemValue="id" itemLabel="fullName" />
					</form:select>
				</div>
				<form:label class="col-sm-2 control-label" path="datePublished">Publizéiert</form:label>
				<div class="col-sm-2">
					<form:input path="datePublished" cssErrorClass="errors form-control" cssClass="form-control" type="date" />
				</div>
				<form:label class="col-sm-2 control-label" path="published">Siichtbar</form:label>
				<div class="col-sm-2">
					<form:checkbox path="published" />
				</div>
			</div>

			<hr />

			<div class="form-group">
				<%--
					<form:label class="col-sm-2 control-label" path="content">Text</form:label>
				--%>
				<div class="col-xs-12">
					<form:textarea path="content" cssErrorClass="errors form-control" cssClass="form-control" />
				</div>
			</div>

			<div class="form-group">
				<div class="col-xs-offset-2 col-xs-5">
					<button type="submit" class="btn btn-primary">Späicheren</button>
				</div>
			</div>
		</form:form>

	</article>
</div>

<script type="text/javascript">
	$(document).ready(function() {
		CKEDITOR.replace("content", {
			extraPlugins : "autogrow"
		});
	});
</script>


<%@ include file="../_footer.jsp"%>
