<div id="removeRepresentativeDialog" title="<g:message code="removeRepresentativeLbl"/>"  style="padding:20px 20px 20px 20px">
	<p style="text-align: center;"><g:message code="removeRepresentativeMsg"/></p>
	<p style="text-align: center;"><g:message code="clickAcceptToContinueLbl"/></p>
</div> 
<script>

$("#removeRepresentativeDialog").dialog({
   	  width: 450, autoOpen: false, modal: true,
      buttons: [{
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		$("#removeRepresentativeDialog").dialog("close");
	             	removeRepresentative() 
	             }},{
        		text:"<g:message code="cancelLbl"/>",
               	icons: { primary: "ui-icon-closethick"},
             	click:function() {
	   				$(this).dialog( "close" );
	       	 	}}],
      show: { effect: "fade", duration: 100 },
      hide: { effect: "fade", duration: 100 }
    });

function removeRepresentative() {
	console.log("removeRepresentative")
   	var webAppMessage = new WebAppMessage(
	    	StatusCode.SC_PROCESANDO, 
	    	Operation.REPRESENTATIVE_REVOKE)
   	webAppMessage.nombreDestinatarioFirma="${grailsApplication.config.SistemaVotacion.serverName}"
	webAppMessage.urlServer="${grailsApplication.config.grails.serverURL}"
	webAppMessage.contenidoFirma = {operation:Operation.REPRESENTATIVE_REVOKE}
	webAppMessage.urlTimeStampServer = "${createLink( controller:'timeStamp', absolute:true)}"
	webAppMessage.urlEnvioDocumento = "${createLink(controller:'representative', action:'revoke', absolute:true)}"
	webAppMessage.asuntoMensajeFirmado = '<g:message code="removeRepresentativeMsgSubject"/>'
	webAppMessage.respuestaConRecibo = true
	votingSystemClient.setMessageToSignatureClient(webAppMessage, removeRepresentativeCallback); 
}

function removeRepresentativeCallback(appMessage) {
	console.log("removeRepresentativeCallback - message from native client: " + appMessage);
	var appMessageJSON = toJSON(appMessage)
	if(appMessageJSON != null) {
		$("#workingWithAppletDialog" ).dialog("close");
		var caption = '<g:message code="operationERRORCaption"/>'
		var msg = appMessageJSON.mensaje
		if(StatusCode.SC_OK == appMessageJSON.codigoEstado) { 
			caption = "<g:message code='operationOKCaption'/>"
			msg = "<g:message code='removeRepresentativeOKMsg'/>";
		} else if (StatusCode.SC_CANCELADO== appMessageJSON.codigoEstado) {
			caption = "<g:message code='operationCANCELLEDLbl'/>"
		}
		showResultDialog(caption, msg)
	}
}

</script>