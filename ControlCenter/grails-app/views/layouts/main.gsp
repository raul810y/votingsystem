<!DOCTYPE html>
<html>
<head>   
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><g:message code="nombreServidorLabel"/></title>
	<r:require module="application"/>
	<g:layoutHead/>
	<r:layoutResources />
</head>
<body>
	<div class="header">
		<div class="col-subsystem" style="width:200px;">
			<g:img dir="images" file="feed.png" style="margin:3px 0 0 15px;"></g:img>
			<g:link controller="subscripcion" action="votaciones" style="font-size: 0.8em; display:inline;"><g:message code="subscribeToFeedsLbl"/></g:link>
		</div>
	   	<div id="selectedSubsystemDiv" class="col-selectedSystem"  style="width:300px;">
	        <a id="selectedSubsystemLink"></a>
	   	</div>
	   	<div class="col-advancedSearch">
			<form id="searchForm" style="width:300px;">
			  	<input name="q" placeholder="<g:message code="searchLbl"/>" style="width:120px;">
				<div id="advancedSearchLink" class="appLink" style="display:inline;font-weight:bold; font-size: 0.8em;">
					<g:message code="advancedSearchLabel"/>
				</div>
			</form>
	   	</div>
	</div>

	<g:layoutBody/>

	<div class="footer" style="display:none;width:100%;">
		<a class="appLink" href="mailto:${grailsApplication.config.VotingSystem.emailAdmin}"
			 style="">${message(code: 'emailLabel', null)}</a>
		<a class="appLink" href="${createLink(controller: 'infoServidor', action: 'informacion')}"
			style="margin: 3px 0 0 20px; float:right;">
			<g:message code="dataInfoLinkText"/>
		</a>
	</div>
		
	<g:include view="/include/dialog/advancedSearchDialog.gsp"/>			
</body>
</html>
<r:script>
	        
	$(function() {		 	
		updateSubsystem("${selectedSubsystem}")
		$(".footer").fadeIn(3000)
		$("#advancedSearchLink").click(function () { 
			$("#advancedSearchDialog").dialog("open");
		});

		 $('#searchForm').submit(function(event){
		 	console.log("searchForm")
		 	event.preventDefault();
		 	var searchQuery = {textQuery:$("#searchText").val()}
		 	getSearchResult(searchQuery)
		 });
	})
	
	
	function setMessageFromSignatureClient(appMessage) {
		var appMessageJSON = toJSON(appMessage)
		if(appMessageJSON != null) {
			if(StatusCode.SC_PROCESANDO == appMessageJSON.codigoEstado){
				signatureClientToolLoaded = true;
				$("#loadingVotingSystemAppletDialog").dialog("close");
				$("#workingWithAppletDialog").dialog("open");
			}
		}
	}
			 
</r:script>
<r:layoutResources/>