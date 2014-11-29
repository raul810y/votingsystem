<%@ page import="grails.converters.JSON" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <vs:webcss dir="font-awesome/css" file="font-awesome.min.css"/>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <asset:stylesheet src="cooins.css"/>
</head>
<body>
<div class="pageContentDiv" style="max-width: 1000px; padding: 20px 30px 0px 30px;">

    <div layout horizontal center center-justified>
        <select id="messagevsStateSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                onchange="messagevsStateSelect(this)">
            <option value="PENDING"> - <g:message code="selectPendingMessaVSLbl"/> - </option>
            <option value="CONSUMED"> - <g:message code="selectConsumedMessaVSLbl"/> - </option>
        </select>
    </div>

    <div id="adviceMessageDiv"><g:message code="messageVSPendingMsg"/></div>


    <div id="messagevs_tableDiv" style="margin: 20px auto 0px auto; max-width: 1200px; overflow:auto;">
        <table class="table tableHeadervs" id="messagevs_table" style="">
            <thead>
            <tr style="color: #ff0000;">
                <th style="width:270px;"><g:message code="dateLbl"/></th>
                <th style="width:270px;"><g:message code="fromUserLbl"/></th>
                <th style="width:270px;"></th>
            </tr>
            </thead>
        </table>
    </div>

</div>
<div id="messageVSTemplate" class="text-center"></div>
</body>
</html>
<asset:script>

    //var messageVSList = ${raw(messageVSList)}

    var messageVSTemplate = document.getElementById("messageVSTemplate").innerHTML
    var messageVSMap = {}
    var messageVSTable = document.getElementById("messagevs_table")


    function setMessageVSList(messageVSList) {
        for(var i = 0; i < messageVSList.length ; i++) {
            //fromUser:[id:messageVS.fromUserVS.id, name:messageVS.fromUserVS.name]
            //messageVSList.add([fromUser: fromUser, dateCreated:messageVS.dateCreated,
            //encryptedDataList:messageVSJSON.encryptedDataList]
            var messageVS = messageVSList[i]
            messageVSMap[messageVS.id] = messageVS

            //console.log(" - messageVS: " +  JSON.stringify(messageVSMap))
            var row = messageVSTable.insertRow(1);
            var cell1 = row.insertCell(0);
            var cell2 = row.insertCell(1);
            var cell3 = row.insertCell(2);
            cell1.innerHTML = '<tr><td title="" class="text-center">' + messageVS.dateCreated + '</td>';
            cell2.innerHTML = '<td title="" class="text-center">' + messageVS.fromUser.name + '</td>';
            cell3.innerHTML = '<td class="text-center"><button onclick="decryptMessageVS(' + messageVS.id + ')">' +
                '<g:message code="readMessageVSLbl"/></button></td></tr>';
        }

    }

    function updateMessageVSList(appMessage) {
        var appMessageJSON = toJSON(appMessage)
        if('PENDING' == appMessageJSON.state) {
            document.getElementById("adviceMessageDiv").innerHTML = '<g:message code="messageVSPendingMsg"/>'
        } else if('CONSUMED' == appMessageJSON.state) {
            document.getElementById("adviceMessageDiv").innerHTML = ''
        }

        var elmtTable = document.getElementById('messagevs_table');
        var tableRows = elmtTable.getElementsByTagName('tr');
        var rowCount = tableRows.length;
        for (var x=0; x < rowCount-1; x++) {
           document.getElementById("messagevs_table").deleteRow(1);
        }


        setMessageVSList(appMessageJSON.messageVSList)
    }

    function messagevsStateSelect(selected) {
        var optionSelected = selected.value
        var webAppMessage = new WebAppMessage(Operation.MESSAGEVS_GET)
        webAppMessage.signedMessageSubject = "<g:message code="getMessageSubject"/>"
        webAppMessage.document = {operation:Operation.MESSAGEVS_GET, state:optionSelected}
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

    function decryptMessageVS(messageVSId) {
        console.log("decryptMessageVS: " + messageVSId)
        var webAppMessage = new WebAppMessage(Operation.MESSAGEVS_DECRYPT)
        webAppMessage.signedMessageSubject = "<g:message code="decryptMessageSubject"/>"
        webAppMessage.setCallback(function(appMessage) {
            var messageVS = JSON.parse(appMessage)
            showMessageVS(messageVS.messageContent, '<g:message code="messageVSDecryptedCaption"/>')
        })
        webAppMessage.documentToDecrypt = messageVSMap[messageVSId]
        VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
    }

</asset:script>
<asset:deferredScripts/>