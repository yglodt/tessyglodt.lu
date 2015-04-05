<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

</div>

<div class="container">
	<footer class="darkgradient">
		<p>&copy; 2012 - <fmt:formatDate pattern="yyyy" value="${now}"/> Tessy Glodt</p>
	</footer>
</div>

<c:choose>
	<c:when test="${fn:contains(pageContext.request.serverName, 'tessyglodt.lu') and empty param.nocombine}">
		<script src="<c:url value='/resources/js/$dynamicResourceNamePart$.min.js' />" type="text/javascript"></script>
	</c:when>
	<c:otherwise>
		<script type="text/javascript" src="<c:url value='/resources/js/bootstrap-without-jquery.js' />"></script>
		<%--
		<script type="text/javascript" src="<c:url value='/resources/js/jquery.js' />"></script>
		<script type="text/javascript" src="<c:url value='/resources/js/bootstrap.js' />"></script>
		<script type="text/javascript" src="<c:url value='/resources/js/script.js' />"></script>
		--%>
	</c:otherwise>
</c:choose>

</body>
</html>
