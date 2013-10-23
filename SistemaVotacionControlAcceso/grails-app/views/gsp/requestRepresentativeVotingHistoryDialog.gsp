<div id="requestRepresentativeVotingHistoryDialog" title="<g:message code="requestVotingHistoryLbl"/>" style="padding:20px 20px 20px 20px;display:none;">
	<g:message code="representativeHistoryRequestMsg"/>
	<label><g:message code="selectDateRangeMsg"/></label>
	<form id="reqVotingHistoryForm">
		<input type="hidden" autofocus="autofocus" />
		<div style="display:table;margin:20px 0px 0px 0px;">
			<div style="display:table-cell;margin:0px 0px 0px 20px;">
				<input type="text" id="dateFrom" style="width:200px;" readonly required
				title='<g:message code='firstDaterangeLbl'/>'
				placeholder='<g:message code='firstDaterangeLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
				onchange="this.setCustomValidity('')"/>
			</div>
			<div style="display:table-cell;margin:0px 0px 0px 20px;">
				<input type="text" id="dateTo" style="width:200px;margin:0px 0px 0px 20px;" readonly required
				title='<g:message code='dateToLbl'/>'
				placeholder='<g:message code='dateToLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emptyFieldLbl"/>')"
				onchange="this.setCustomValidity('')"/>
			</div>
		</div>
		<div style="margin:15px 0px 20px 0px">
			<input type="email" id="userEmailText" style="width:350px; margin:0px auto 0px auto;" required
				title='<g:message code='enterEmailLbl'/>'
				placeholder='<g:message code='emailInputLbl'/>'
				oninvalid="this.setCustomValidity('<g:message code="emailERRORMsg"/>')"
				onchange="this.setCustomValidity('')"/>
		</div>
		<input id="submitVotingHistoryRequest" type="submit" style="display:none;">
	</form> 	   
</div> 
<script>
$("#dateFrom").datepicker(pickerOpts);
$("#dateTo").datepicker(pickerOpts);

  $("#requestRepresentativeVotingHistoryDialog").dialog({
   	  width: 550, autoOpen: false, modal: true,
      buttons: [{id: "acceptButton",
        		text:"<g:message code="acceptLbl"/>",
               	icons: { primary: "ui-icon-check"},
             	click:function() {
             		$("#submitVotingHistoryRequest").click() 	   			   				
	        	}}, {id: "cancelButton",
		        		text:"<g:message code="cancelLbl"/>",
		               	icons: { primary: "ui-icon-closethick"},
		             	click:function() {
   			   					$(this).dialog( "close" );
   			       	 		}}],
      show: {effect:"fade", duration: 300},
      hide: {effect: "fade",duration: 300}
});
</script>