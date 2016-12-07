<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="../_header.jsp"%>

<div class="box margin-top margin-bottom">
	<article class="text-box">
		<h4>Distrikt ${empty param.id ? 'derbäisetzen' : 'änneren' }</h4>

		<form:form modelAttribute="district" method="post" action="${pageContext.request.contextPath}/admin/districtform" class="form-horizontal">

			<form:hidden path="id" />

			<div class="form-group">
				<form:label class="col-sm-2 control-label" path="name">Numm</form:label>
				<div class="col-sm-2">
					<form:input path="name" cssErrorClass="errors form-control" cssClass="form-control" />
				</div>
			</div>

			<div class="form-group">
				<div class="col-xs-offset-2 col-xs-10">
					<button type="submit" class="btn btn-primary">Späicheren</button>
				</div>
			</div>
		</form:form>

	</article>
</div>

<%@ include file="../_footer.jsp"%>
