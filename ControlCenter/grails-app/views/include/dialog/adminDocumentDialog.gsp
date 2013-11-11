<div id="adminDocumentDialog" title="<g:message code="confirmOptionDialogCaption"/>">	    	
	<p style="text-align: center;"><g:message code="adminDocumenInfoMsg"/></p>
	<g:message code="documentStateSelectionMsg"/>:<br/>
	<div style="font-size: 0.9em; margin:10px 0 0 10px;"> 
		<div style="margin:0px 0 10px 0px;display:block;">
			<input type="checkbox" id="selectDeleteDocument"><g:message code="selectDeleteDocumentMsg"/>
		</div>
		<input type="checkbox" id="selectCloseDocument"><g:message code="selectCloseDocumentMsg"/>
	</div>
</div> 
<script>
$("#selectDeleteDocument").click(function () {
	if($("#selectCloseDocument").is(':checked')) {
		$("#selectCloseDocument").prop('checked', false);
	}
})

$("#selectCloseDocument").click(function () {
	if($("#selectDeleteDocument").is(':checked')) {
		$("#selectDeleteDocument").prop('checked', false);
	}
})

$("#adminDocumentDialog").dialog({
 	  width: 600, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		submitAdminForm() 	
             		$(this).dialog( "close" );   	   			   				
 			        	}
           },
           {
        		text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
		   					$(this).dialog( "close" );
		       	 		}	
           }],
      show: {effect:"fade", duration: 300},
      hide: {effect: "fade",duration: 300}
    });

function submitAdminForm() {
	console.log("submitAdminForm")
	if(!$("#selectDeleteDocument").is(':checked') &&
			!$("#selectCloseDocument").is(':checked')) {
		showResultDialog("<g:message code='errorLbl'/>", 
				"<g:message code='selectDocumentStateERRORMsg'/>")
	} else {
		var estado
		if($("#selectDeleteDocument").is(':checked')) {
			estado = DocumentState.BORRADO_DE_SISTEMA
		} else if($("#selectCloseDocument").is(':checked')) {
			estado = DocumentState.CANCELADO
		}
    	var webAppMessage = new WebAppMessage(
		    	StatusCode.SC_PROCESANDO, 
		    	Operation.CANCELAR_EVENTO)
    	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.VotingSystem.serverName}"
		webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
		webAppMessage.respuestaConRecibo = false
		webAppMessage.urlTimeStampServer = "${createLink(controller:'timeStamp', absolute:true)}"
		webAppMessage.urlEnvioDocumento= "${createLink(controller:'evento', action:'cancelled', absolute:true)}"
		var contenidoFirma = {operation:Operation.CANCELAR_EVENTO,
				accessControlURL:"${grailsApplication.config.grails.serverURL}",
				eventId:"${eventMap?.id}", estado:estado}
		webAppMessage.contenidoFirma = contenidoFirma
		pendingOperation = Operation.CANCELAR_EVENTO
		//console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
		votingSystemClient.setMessageToSignatureClient(JSON.stringify(webAppMessage)); 
	}
}
</script>