<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>

<%@ include file="_header.jsp"%>

<div class="cont main">
	<article>

		<h4>Log in</h4>

		<c:if test="${param.error}">
			<p>Invalid username and password.</p>
		</c:if>

		<c:if test="${param.logout}">
			<p>You have been logged out.</p>
		</c:if>

		<form class="form-horizontal" action="<c:url value='/authcheck' />" method="post">

			<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

			<div class="form-group">
				<label class="control-label col-sm-2" for="username">Username</label>
				<div class="col-sm-2">
					<input type="text" id="username" name="username" class="form-control">
				</div>
			</div>

			<div class="form-group">
				<label class="control-label col-sm-2" for="password">Password</label>
				<div class="col-sm-2">
					<input type="password" id="password" name="password" class="form-control">
				</div>
			</div>

			<div class="form-group">
				<div class="col-sm-offset-2 col-sm-2">
					<button class="btn btn-primary" type="submit">Log in</button>
				</div>
			</div>
		</form>
	</article>
</div>

<script type="text/javascript">
	document.getElementById("username").focus();
</script>

<%@ include file="_footer.jsp"%>
