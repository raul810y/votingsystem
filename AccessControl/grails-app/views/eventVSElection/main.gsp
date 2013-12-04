<html>
<head>
    <meta name="layout" content="main" />
   	<r:require module="paginate"/>
</head>
<body>
<div class="mainPage">
    <div id="contentDiv" style="display:none;">

        <div style="display: table;width:100%;vertical-align: middle;margin:0px 0 10px 0px;">
            <div style="display:table-cell;width:300px;">
                <votingSystem:simpleButton href="${createLink(controller:'representative', action:'main')}" style="margin:0px 0px 0px 15px;">
                    <g:message code="representativesPageLbl"/>
                </votingSystem:simpleButton>
                <div style="margin: 10px 0 0 30px;"><votingSystem:feed  href="${createLink(controller:'subscriptionVS', action:'elections')}">
                    <g:message code="subscribeToFeedsLbl"/>
                    </votingSystem:feed>
                </div>
            </div>
            <div style="display:table-cell;margin: auto; vertical-align: top;">
                <select id="eventsStateSelect" style="margin:0px 0px 0px 40px;color:black;">
                    <option value="" style="color:black;"> - <g:message code="selectPollsLbl"/> - </option>
                    <option value="ACTIVE" style="color:#6bad74;"> - <g:message code="selectOpenPollsLbl"/> - </option>
                    <option value="AWAITING" style="color:#fba131;"> - <g:message code="selectPendingPollsLbl"/> - </option>
                    <option value="TERMINATED" style="color:#cc1606;"> - <g:message code="selectClosedPollsLbl"/> - </option>
                </select>
            </div>

            <div style="display:table-cell; vertical-align: top;">
                <votingSystem:simpleButton href="${createLink(controller:'editor', action:'vote')}" style="margin:0px 20px 0px 0px;">
                    <g:message code="publishDocumentLbl"/>
                </votingSystem:simpleButton>
            </div>
        </div>

    </div>

    <g:render template="/template/eventsSearchInfo"/>

    <div id="progressDiv" style="position: absolute; left: 40%; right:40%; top: 300px;">
        <progress style="margin:0px auto 0px auto;"></progress>
    </div>

    <div id="mainPageEventList" class="mainPageEventList"><ul></ul></div>

    <g:render template="/template/pagination"/>


    <div id="eventTemplate" style="display:none;">
        <g:render template="/template/event" model="[isTemplate:'true']"/>
    </div>
</div>
</body>
</html>
<r:script>
        	var eventState = ''
            var searchQuery
		 	$(function() {
		 		paginate(0)
		 		$('#eventsStateSelect').on('change', function (e) {
		 			eventState = $(this).val()
		 		    var optionSelected = $("option:selected", this);
		 		    console.log(" - eventState: " + eventState)
		 		    if(!isFirefox()) {
			 		    if($('#eventsStateSelect')[0].selectedIndex == 0) {
			 		    	$('#eventsStateSelect').css({'color': '#434343', 'border-color': '#cccccc'})
				 		} else {
			 		    	$('#eventsStateSelect').css({'color': $( "#eventsStateSelect option:selected" ).css('color'),
    							 'border-color': $( "#eventsStateSelect option:selected" ).css('color')})
					 	}
			 		}
					var targetURL = "${createLink( controller:'eventVS')}"
					if("" != eventState) targetURL = targetURL + "?eventVSState=" + $(this).val()
		 		    loadEvents(targetURL)
		 		});
				$("#searchFormDiv").fadeIn()
			 });

			function loadEvents(eventsURL, data) {
				console.log("- loadEvents - eventsURL: " + eventsURL);
				var requestType = 'GET'
				if(data != null) requestType = 'POST'
				var $loadingPanel = $('#progressDiv')
				var $contentDiv = $('#contentDiv')
				$contentDiv.css("display", "none")
				$('#mainPageEventList ul').empty()
				$loadingPanel.fadeIn(100)
				$.ajax({
					url: eventsURL,
					type:requestType,
					contentType:'application/json',
					data: JSON.stringify(data),
				}).done(function(jsonResult) {
					console.log(" - ajax call done - printEvents");
					printEvents(jsonResult)
				}).error(function() {
					console.log("- ajax error - ");
					showResultDialog('<g:message code="errorLbl"/>',
						'<g:message code="connectionERRORMsg"/>') 
					$loadingPanel.fadeOut(100)
				});
			}

			var eventTemplate = $('#eventTemplate').html()

			function printEvents(eventsJSON) {
				$.each(eventsJSON.eventsVS.elections, function() {
                    var eventVS = new EventVS(this, eventTemplate, "${selectedSubsystem}")
				    $("#mainPageEventList ul").append(eventVS.getElement())
				});
				printPaginate(eventsJSON.offset, eventsJSON.numEventsVSElectionInSystem, numMaxEventsForPage)
				$('#contentDiv').fadeIn(500)
				$('#progressDiv').fadeOut(500)
			}

			function paginate (newOffsetPage) {
				console.log(" - paginate - offsetPage : " + offsetPage + " - newOffsetPage: " + newOffsetPage)
				if(newOffsetPage == offsetPage) return
				offsetPage = newOffsetPage
				var offsetItem
				if(newOffsetPage == 0) offsetItem = 0
				else offsetItem = (newOffsetPage -1) * numMaxEventsForPage
				var targetURL = "${createLink( controller:'eventVS')}?max=" + numMaxEventsForPage + "&offset=" + offsetItem
				if(searchQuery != null) targetURL = "${createLink( controller:'search', action:'find')}?max=" +
						numMaxEventsForPage + "&offset=" + offsetItem
				loadEvents(targetURL, searchQuery)	
			}

			function getSearchResult(newSearchQuery) {
				newSearchQuery.eventState = eventState
				newSearchQuery.subsystem = "${selectedSubsystem}"
				searchQuery = newSearchQuery
				showEventsSearchInfoMsg(newSearchQuery)
				loadEvents("${createLink(controller:'search', action:'find')}?max=" +
						numMaxEventsForPage + "&offset=0", newSearchQuery)
			}
</r:script>