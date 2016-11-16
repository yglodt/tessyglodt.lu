<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ include file="_header.jsp"%>

<div class="cont main">
	<%@ include file="_randomPage.jsp"%>
	<div class="fitem">
		<h4 style="text-align: center;">Kaart</h4>
		<a href="<c:url value='/kaart' />">
			<img id="index_mapimage" src="<c:url value='/resources/img/kaart.jpg' />" width="300" height="164">
		</a>
	</div>
	<div class="fitem blizzy">
		<h4>Eppes neies...</h4>
		<div id="text blizzy">
			<a href="<c:url value='/blizzy' />">
				<img id="bimage" src="<c:url value='/resources/b/ad-lb.jpg' />" width="300" height="162">
			</a>
		</div>
	</div>
</div>

<div class="cont main">
	<article>
		<h4>All d'Texter</h4>
		<div class="textcolumns">
			<%@ include file="_pagesInfos.jsp"%>
		</div>
	</article>
	<aside>
		<%@ include file="_newestPages.jsp"%>
		<%@ include file="_lastReadPages.jsp"%>
		<%@ include file="_mostReadPages.jsp"%>
		<%@ include file="_districts.jsp"%>
		<%@ include file="_cantons.jsp"%>
	</aside>
</div>

<script type="text/javascript">
window.setInterval(function() {
	var i = document.getElementById("bimage");
	if (i.src.substring(i.src.length - 22) === "/resources/b/ad-lb.jpg") {
		i.src = "/resources/b/ad-fr.jpg";
	} else {
		i.src = "/resources/b/ad-lb.jpg";
	}
}, 5000);
</script>

<%@ include file="_footer.jsp"%>
