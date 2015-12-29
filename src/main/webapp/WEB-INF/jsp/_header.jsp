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
<link href='//fonts.googleapis.com/css?family=Italianno' rel='stylesheet' type='text/css'>
<link href='//fonts.googleapis.com/css?family=Smythe' rel='stylesheet' type='text/css'>
<c:choose>
	<c:when test="${fn:contains(pageContext.request.serverName, 'tessyglodt.lu') and empty param.nocombine}">
		<link href="<c:url value='/resources/css/$dynamicResourceNamePart$.min.css' />" rel="stylesheet" type="text/css" />
	</c:when>
	<c:otherwise>
		<link rel="stylesheet" href="<c:url value='/resources/css/bootstrap.css' />">
		<link rel="stylesheet" href="<c:url value='/resources/css/bootstrap-theme.css' />">
		<link rel="stylesheet" href="<c:url value='/resources/css/style.css' />">
	</c:otherwise>
</c:choose>
<link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png' />" sizes="64x64">
<c:if test="${fn:contains(pageContext.request.serverName, 'tessyglodt.lu')}">
	<script type="text/javascript">
		var _gaq = _gaq || [];
		_gaq.push([ '_setAccount', 'UA-29144750-1' ]);
		_gaq.push([ '_trackPageview' ]);

		(function() {
			var ga = document.createElement('script');
			ga.type = 'text/javascript';
			ga.async = true;
			ga.src = ('https:' == document.location.protocol ? 'https://ssl'
					: 'http://www')
					+ '.google-analytics.com/ga.js';
			var s = document.getElementsByTagName('script')[0];
			s.parentNode.insertBefore(ga, s);
		})();
	</script>
</c:if>
<link href="<c:url value='/feed/nei.xml' />" title="Nei Texter" type="application/atom+xml" rel="alternate" />
</head>
<body>
	<%-- Facebook SDK (for "like" button) --%>
	<div id="fb-root"></div>
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
	<br class="hidden-xs" />
	<div class="container headermain">
		<div style="margin-top: 10px; height: 20px; text-align: right;">
			<div class="fb-like" data-href="https://www.facebook.com/Kierchtuermspromenaden" data-send="false" data-layout="button_count" data-width="30" data-show-faces="true"></div>
		</div>
		<header style="margin-top: -10px;">
			<h1>Kierchtuerms&shy;promenaden</h1>
			<h2 class="subtitle">En Tour duerch d&nbsp;'Lëtzebuerger Land mam Tessy Glodt</h2>
		</header>
	</div>

	<div class="container main">
		<br />
		<div class="navbar navbar-default hidden-print">
			<div class="container-fluid">
				<div class="navbar-header">
					<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target=".navbar-collapse">
						<span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
					</button>
					<a class="navbar-brand online" href="<c:url value='/' />">Haaptsäit</a>
				</div>

				<div class="navbar-collapse collapse">
					<ul class="nav navbar-nav">
						<li><a href="<c:url value='/kaart' />">Kaart mat den Uertschaften</a></li>
						<li><a href="<c:url value='/apropos' />">Iwwert des Säit</a></li>
						<li><a href="<c:url value='/auteur' />">Iwwert den Auteur</a></li>
						<sec:authorize access="isAuthenticated()">
							<li><a href="<c:url value='/admin/pageform' />">Nei Säit</a></li>
							<li><a href="<c:url value='/admin/listmcd' />">Konfig</a></li>
							<li><a href="<c:url value='/logout' />">Log out</a></li>
						</sec:authorize>
					</ul>

					<form class="navbar-form navbar-right" role="search" action="${pageContext.request.contextPath}/sich">
						<div class="form-group">
							<input type="text" name="q" class="form-control input-sm" placeholder="Sichbegrëff" value="${param.q}" size="10">&nbsp;
						</div>
						<button type="submit" class="btn btn-default btn-sm">Sichen</button>
					</form>
				</div>
			</div>
		</div>