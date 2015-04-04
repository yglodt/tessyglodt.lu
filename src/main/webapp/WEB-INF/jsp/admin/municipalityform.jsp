<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="../_header.jsp"%>

<h4>Gemeng ${empty param.id ? 'derbäisetzen' : 'änneren' }</h4>

<form:form modelAttribute="municipality" method="post" action="${pageContext.request.contextPath}/admin/municipalityform" class="form-horizontal">

	<form:hidden path="id" />

	<div class="form-group">
		<form:label class="col-sm-2 control-label" path="name">Numm</form:label>
		<div class="col-sm-2">
			<form:input path="name" cssErrorClass="errors form-control" cssClass="form-control" />
		</div>
	</div>

	<div class="form-group">
		<form:label class="col-sm-2 control-label" path="canton">Kanton</form:label>
		<div class="col-sm-2">
			<form:select path="canton" cssClass="form-control" cssErrorClass="errors form-control">
				<form:option value="">[...]</form:option>
				<form:options items="${cantons}" itemValue="id" itemLabel="name" />
			</form:select>
		</div>
	</div>

	<div class="form-group">
		<div class="col-xs-offset-2 col-xs-10">
			<button type="submit" class="btn btn-primary">Späicheren</button>
		</div>
	</div>
</form:form>

<%@ include file="../_footer.jsp"%>
