<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<jsp:useBean id="now" class="java.util.Date" scope="page" />
<!DOCTYPE html>
<html lang="lb">
<head>
<title>Blizzy</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
<meta name="theme-color" content="green">
<link rel="stylesheet" href="<c:url value='/resources/css/bootstrap.css' />">
<link rel="stylesheet" href="<c:url value='/resources/css/bootstrap-theme.css' />">
<script type="text/javascript" src="<c:url value='/resources/js/jquery.min.js' />"></script>
<script type="text/javascript" src="<c:url value='/resources/js/jquery.validate.min.js' />"></script>
<style type="text/css">
.errors {
	border-color: red;
	background-color: #f9dede
}
</style>
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
</head>
<body>

	<div class="container">

		<div class="row">

			<div class="col-sm-4">
				<h1>Blizzy</h1>
				<p>Latius iam disseminata licentia onerosus bonis omnibus Caesar nullum post haec adhibens modum orientis latera cuncta vexabat nec honoratis parcens nec urbium primatibus nec plebeiis.</p>
				<p>Sed ut tum ad senem senex de senectute, sic hoc libro ad amicum amicissimus scripsi de amicitia. Tum est Cato locutus, quo erat nemo fere senior temporibus illis, nemo prudentior; nunc Laelius et sapiens (sic enim est habitus) et amicitiae gloria excellens de amicitia loquetur. Tu velim a me animum parumper avertas, Laelium loqui ipsum putes. C. Fannius et Q. Mucius ad socerum veniunt post mortem Africani; ab his sermo oritur, respondet Laelius, cuius tota disputatio est de amicitia, quam legens te ipse cognosces.</p>
				<p>Coactique aliquotiens nostri pedites ad eos persequendos scandere clivos sublimes etiam si lapsantibus plantis fruticeta prensando vel dumos ad vertices venerint summos, inter arta tamen et invia nullas acies explicare permissi nec firmare nisu valido gressus: hoste discursatore rupium abscisa volvente, ruinis ponderum inmanium consternuntur, aut ex necessitate ultima fortiter dimicante, superati periculose per prona discedunt.</p>
				<p>Vita est illis semper in fuga uxoresque mercenariae conductae ad tempus ex pacto atque, ut sit species matrimonii, dotis nomine futura coniunx hastam et tabernaculum offert marito, post statum diem si id elegerit discessura, et incredibile est quo ardore apud eos in venerem uterque solvitur sexus.</p>
				<p>Proinde concepta rabie saeviore, quam desperatio incendebat et fames, amplificatis viribus ardore incohibili in excidium urbium matris Seleuciae efferebantur, quam comes tuebatur Castricius tresque legiones bellicis sudoribus induratae.</p>
			</div>

			<div class="col-sm-4">
				<h1>&nbsp;</h1>
				<p>Latius iam disseminata licentia onerosus bonis omnibus Caesar nullum post haec adhibens modum orientis latera cuncta vexabat nec honoratis parcens nec urbium primatibus nec plebeiis.</p>
				<p>Sed ut tum ad senem senex de senectute, sic hoc libro ad amicum amicissimus scripsi de amicitia. Tum est Cato locutus, quo erat nemo fere senior temporibus illis, nemo prudentior; nunc Laelius et sapiens (sic enim est habitus) et amicitiae gloria excellens de amicitia loquetur. Tu velim a me animum parumper avertas, Laelium loqui ipsum putes. C. Fannius et Q. Mucius ad socerum veniunt post mortem Africani; ab his sermo oritur, respondet Laelius, cuius tota disputatio est de amicitia, quam legens te ipse cognosces.</p>
				<p>Coactique aliquotiens nostri pedites ad eos persequendos scandere clivos sublimes etiam si lapsantibus plantis fruticeta prensando vel dumos ad vertices venerint summos, inter arta tamen et invia nullas acies explicare permissi nec firmare nisu valido gressus: hoste discursatore rupium abscisa volvente, ruinis ponderum inmanium consternuntur, aut ex necessitate ultima fortiter dimicante, superati periculose per prona discedunt.</p>
				<p>Vita est illis semper in fuga uxoresque mercenariae conductae ad tempus ex pacto atque, ut sit species matrimonii, dotis nomine futura coniunx hastam et tabernaculum offert marito, post statum diem si id elegerit discessura, et incredibile est quo ardore apud eos in venerem uterque solvitur sexus.</p>
				<p>Proinde concepta rabie saeviore, quam desperatio incendebat et fames, amplificatis viribus ardore incohibili in excidium urbium matris Seleuciae efferebantur, quam comes tuebatur Castricius tresque legiones bellicis sudoribus induratae.</p>
			</div>

			<div class="col-sm-4">
				<h1>Commande</h1>

				<form:form modelAttribute="order" method="post" action="${pageContext.request.contextPath}/blizzy" class="form-horizontal">

					<div class="form-group">
						<form:label path="title">Titel / Titre</form:label>
						<form:select path="title" cssClass="form-control">
							<form:option value="">[...]</form:option>
							<form:option value="MS">Madame</form:option>
							<form:option value="MR">Monsieur</form:option>
						</form:select>
					</div>

					<div class="form-group">
						<form:label path="lastName">Nonumm / Nom de famille</form:label>
						<form:input path="lastName" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="firstName">Virnumm / Prénom</form:label>
						<form:input path="firstName" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="email">Email</form:label>
						<form:input type="email" path="email" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="houseNumber">Hausnummer / Numéro</form:label>
						<form:input path="houseNumber" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="street">Strooss / Rue</form:label>
						<form:input path="street" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="zipCode">Postleitzuel / Code postal</form:label>
						<form:input path="zipCode" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="city">Uertschaft / Ville</form:label>
						<form:input path="city" cssClass="form-control" />
					</div>

					<div class="form-group">
						<form:label path="country">Land / Pays</form:label>
						<form:select path="country" cssClass="form-control">
							<form:option value="">[...]</form:option>
							<form:option value="lu">Lëtzebuerg / Luxembourg</form:option>
							<form:option value="be">Belsch / Belgique</form:option>
							<form:option value="de">Däitschland / Allemagne</form:option>
							<form:option value="fr">Frankräich / France</form:option>
						</form:select>
					</div>

					<div class="form-group">
						<form:label path="orderCopies">Exemplairen / Exemplaires</form:label>
						<form:input type="number" path="orderCopies" cssClass="form-control" min="0" max="10" />
					</div>

					<div class="form-group">
						<div class="col-xs-6">
							<button type="submit" class="btn btn-primary">Bestellen / Commander</button>
						</div>
					</div>
				</form:form>
			</div>

		</div>

	</div>

	<script type="text/javascript">
		$(document).ready(function() {

			$("h1").click(function() {
				$("#title").val("MS");
				$("#lastName").val("Schmidt");
				$("#firstName").val("Marie-Gelatine");
				$("#email").val("yg@mind.lu");
				$("#houseNumber").val("54");
				$("#street").val("rue de la Gare");
				$("#zipCode").val("1419");
				$("#city").val("Luxembourg");
				$("#country").val("lu");
				$("#orderCopies").val("2");
			});

			$("#order").validate({
				rules : {
					title : "required",
					lastName : "required",
					firstName : "required",
					email : {
						required : true,
						email : true
					},
					houseNumber : "required",
					street : "required",
					zipCode : "required",
					city : "required",
					country : "required",
					orderCopies : "required"
				},
				errorClass : "errors",
				errorPlacement : function(error, element) {
					return true;
				}
			});

		});
	</script>

</body>
</html>
