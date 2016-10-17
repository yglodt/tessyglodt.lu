<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="slg" uri="http://github.com/slugify"%>
<jsp:useBean id="now" class="java.util.Date" scope="page" />
<!DOCTYPE html>
<html lang="lb">
<head>
<c:choose>
	<c:when test="${not empty page}">
		<title>Kierchtuermspromenaden &mdash; ${page.title}</title>
	</c:when>
	<c:otherwise>
		<title>Kierchtuermspromenaden &mdash; En Tour duerch d'Lëtzebuerger Land mam Tessy Glodt</title>
	</c:otherwise>
</c:choose>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
<meta name="theme-color" content="#f9f6e1">
<link href='//fonts.googleapis.com/css?family=Italianno|Smythe' rel='stylesheet' type='text/css'>
<c:choose>
	<c:when test="${fn:contains(pageContext.request.serverName, 'tessyglodt.lu') and empty param.nocombine}">
		<link href="<c:url value='/resources/css/$dynamicResourceNamePart$.min.css' />" rel="stylesheet" type="text/css" />
		<%--<script src="<c:url value='/resources/js/$dynamicResourceNamePart$.min.js' />" type="text/javascript"></script>--%>
	</c:when>
	<c:otherwise>
		<link rel="stylesheet" href="<c:url value='/resources/css/style.css' />">
		<%--<script type="text/javascript" src="<c:url value='/resources/js/script.js' />"></script>--%>
	</c:otherwise>
</c:choose>
<link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png' />" sizes="64x64">
<c:if test="${fn:contains(pageContext.request.serverName, 'tessyglodt.lu')}">
	<script type="text/javascript">
		var _gaq = _gaq || []; _gaq.push([ '_setAccount', 'UA-29144750-1' ]); _gaq.push([ '_trackPageview' ]);
		(function() {
			var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
			ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
			var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
		})();
	</script>
</c:if>
<link href="<c:url value='/feed/nei.xml' />" title="Nei Texter" type="application/atom+xml" rel="alternate" />
</head>
<body>
	<%-- Facebook SDK (for "like" button) --%>
	<script>
		(function(d, s, id) {
			var js, fjs = d.getElementsByTagName(s)[0];
			if (d.getElementById(id))
				return;
			js = d.createElement(s);
			js.id = id;
			js.src = "//connect.facebook.net/en_US/all.js#xfbml=1";
			fjs.parentNode.insertBefore(js, fjs);
		}(document, 'script', 'facebook-jssdk'));
	</script>
	<%-- end facebook SDK --%>
	<div class="cont headermain">
		<header>
			<h1>Kierchtuerms&shy;promenaden</h1>
			<h2 class="subtitle">En Tour duerch d&nbsp;'Lëtzebuerger Land mam Tessy Glodt</h2>
		</header>
		<c:if test="${pageContext.request.requestURI == '/WEB-INF/jsp/index.jsp'}"></c:if>
		<div style="position:absolute; top:15px; right:20px;" class="fb-like" data-href="https://www.facebook.com/Kierchtuermspromenaden" data-send="false" data-layout="button_count" data-width="30" data-show-faces="true"></div>
		<div style="position:absolute; top:50px; right:20px;">
			<a href="https://twitter.com/Kierchtuermspro" class="twitter-follow-button" data-show-count="false">Follow @Kierchtuermspromenaden</a><script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>
		</div>
	</div>

	<nav class="main">
		<a href="<c:url value='/' />">Haaptsäit</a>
		<a href="<c:url value='/kaart' />">Kaart mat den Uertschaften</a>
		<a href="<c:url value='/apropos' />">Iwwert des Säit</a>
		<a href="<c:url value='/auteur' />">Iwwert den Auteur</a>
		<sec:authorize access="isAuthenticated()">
			<a href="<c:url value='/admin/pageform' />">Nei Säit</a>
			<a href="<c:url value='/admin/listmcd' />">Konfig</a>
			<a href="<c:url value='/logout' />">Log out</a>
		</sec:authorize>

		<form role="search" action="${pageContext.request.contextPath}/sich">
			<input type="text" name="q" class="fc" placeholder="Sichbegrëff" value="${param.q}" size="15">&nbsp;
			<button type="submit">Sichen</button>
		</form>
	</nav>