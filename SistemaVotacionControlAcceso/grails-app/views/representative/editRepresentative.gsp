<html>
<head>
        <meta name="layout" content="main" />
       	<r:require modules="textEditorPC"/>
</head>
<body>

<div id="contentDiv" style="display:none;">

	<div class="publishPageTitle">
		<div style="width: 90%; margin: 0 auto;">
			<% def msgParams = [representative.fullName]%>
			<g:message code="editingRepresentativeMsgTitle" args='${msgParams}'/>
		</div>
	</div>
	
	<div class="userAdvert" >
		<ul>
			<li><g:message code="newRepresentativeAdviceMsg1"/></li>
			<li><g:message code="newRepresentativeAdviceMsg2"/></li>
			<li><g:message code="newRepresentativeAdviceMsg3"/></li>
			<li><g:message code="newRepresentativeAdviceMsg4"/></li>
		</ul>
	</div>	
	
	<form id="mainForm">
	
	<votingSystem:textEditorPC id="editorDiv"/>
		
	<div style="position:relative; margin:10px 10px 0px 0px;height:20px;">
		<div style="position:absolute; right:0;">
				<votingSystem:simpleButton isButton='true' 
					imgSrc="${resource(dir:'images',file:'accept_16x16.png')}" style="margin:0px 20px 0px 0px;">
						<g:message code="acceptLbl"/>
				</votingSystem:simpleButton>
		</div>	
	</div>	
		
		
	</form>
		
	<g:render template="/template/signatureMechanismAdvert"  model="${[advices:[message(code:"onlySignedDocumentsMsg")]]}"/>

</div>

</body>
</html>
<r:script>
		 	$(function() {
			    $('#mainForm').submit(function(event){
			    	event.preventDefault();
			    	var editorDiv = $( "#editorDiv" )
			    	editorDiv.removeClass( "ui-state-error" );
					
					if(htmlEditorContent.trim() == 0) {
						editorDiv.addClass( "ui-state-error" );
						showResultDialog('<g:message code="dataFormERRORLbl"/>', 
								'<g:message code="emptyDocumentERRORMsg"/>')
						return false;
					}  

			    	var webAppMessage = new WebAppMessage(
					    	StatusCode.SC_PROCESANDO, 
					    	Operation.NEW_REPRESENTATIVE)
			    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
		    		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
					webAppMessage.contenidoFirma = {representativeInfo:editor.getData(), operation:Operation.REPRESENTATIVE_DATA}
					webAppMessage.urlEnvioDocumento = "${createLink( controller:'representative', absolute:true)}"
					webAppMessage.asuntoMensajeFirmado = '<g:message code="representativeDataLbl"/>'
					webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
					votingSystemClient.setMessageToSignatureClient(webAppMessage, editRepresentativeCallback);
			    	return false 
			    });

			  });


			function editRepresentativeCallback(appMessage) {
				console.log("editRepresentativeCallback - message from native client: " + appMessage);
				var appMessageJSON = toJSON(appMessage)
				if(appMessageJSON != null) {
					$("#workingWithAppletDialog" ).dialog("close");
					var caption = '<g:message code="operationERRORCaption"/>'
					var msg = appMessageJSON.mensaje
					if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
						caption = "<g:message code='operationOKCaption'/>"
					} else if (StatusCode.SC_CANCELADO== appMessageJSON.codigoEstado) {
						caption = "<g:message code='operationCANCELLEDLbl'/>"
					}
					showResultDialog(caption, msg)
				}
			}

</r:script>