<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="javatime" uri="http://sargue.net/jsptags/time" %>
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
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="theme-color" content="#f9f6e1">
<link href='//fonts.googleapis.com/css?family=Italianno|Smythe|Lora|Material+Icons' rel='stylesheet' type='text/css'>
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
  
	<header>
		<h1>Kierchtuerms&shy;promenaden</h1>
		<h2>En Tour duerch d&nbsp;'Lëtzebuerger Land mam Tessy Glodt</h2>
        <div class="fb-like" data-href="https://www.facebook.com/Kierchtuermspromenaden" data-send="false" data-layout="button_count" data-width="30" data-show-faces="true"></div>
	</header>
  
  	<nav>
  		<a href="<c:url value='/' />"><i class="material-icons">home</i>&nbsp;Haaptsäit</a>
  		<a href="<c:url value='/kaart' />"><i class="material-icons">map</i>&nbsp;Kaart mat den Uertschaften</a>
  		<a href="<c:url value='/apropos' />"><i class="material-icons">info_outline</i>&nbsp;Iwwert des Säit</a>
  		<a href="<c:url value='/auteur' />"><i class="material-icons">person</i>&nbsp;Iwwert den Auteur</a>
  
  		<form role="search" action="${pageContext.request.contextPath}/sich">
  			<input type="text" name="q" class="fc" placeholder="Sichbegrëff" value="${param.q}" size="15"><button type="submit"><i class="material-icons">search</i></button>
  		</form>
    </nav>
    <sec:authorize access="isAuthenticated()">
    <nav class="margin-top">
        <a href="<c:url value='/admin/pageform' />">Nei Säit</a>
        <a href="<c:url value='/admin/listmcd' />">Konfig</a>
        <a href="<c:url value='/logout' />">Log out</a>
    </nav>
    </sec:authorize>
  