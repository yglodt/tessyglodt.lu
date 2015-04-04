<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<h4>Zoufallssäit: ${randomPage.title}</h4>
<div id="text">
	<a href="<c:url value='/page/' />${randomPage.name}"> ${fn:substring(randomPage.content,0,empty chars ? 450 : chars) } ... <br /> <span class="weiderliesen">[Weiderliesen]</span></a>
</div>
