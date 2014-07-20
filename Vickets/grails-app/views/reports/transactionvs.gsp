<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <link rel="import" href="${resource(dir: '/bower_components/votingsystem-transaction-table', file: 'votingsystem-transaction-table.html')}">
</head>
<body>
<div class="pageContenDiv">
    <div class="row" style="max-width: 1300px; margin: 0px auto 0px auto;">
        <ol class="breadcrumbVS pull-left">
            <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
            <li class="active"><g:message code="transactionPageTitle"/></li>
        </ol>
    </div>

    <div layout horizontal center center-justified>
        <select id="transactionvsTypeSelect" style="margin:0px auto 0px auto;color:black; max-width: 400px;"
                class="form-control" onchange="transactionvsTypeSelect(this)">
            <option value="" style="color:black;"> - <g:message code="selectTransactionTypeLbl"/> - </option>

            <option value="VICKET_REQUEST"> - <g:message code="selectVicketRequestLbl"/> - </option>
            <option value="VICKET_SEND"> - <g:message code="selectVicketSendLbl"/> - </option>
            <option value="VICKET_CANCELLATION"> - <g:message code="selectVicketCancellationLbl"/> - </option>
        </select>
    </div>

    <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>

    <votingsystem-transaction-table id="recordList" url="${createLink(controller: 'reports', action: 'transactionvs')}"></votingsystem-transaction-table>

</div>
</body>

</html>
<asset:script>
    function transactionvsTypeSelect(selected) {
        var transactionvsType = selected.value
        console.log("transactionvsType: " + transactionvsType)
        if("" != transactionvsType) {
            targetURL = "${createLink(controller: 'reports', action: 'transactionvs')}";
            history.pushState(null, null, targetURL);
            targetURL = targetURL + "?transactionvsType=" + transactionvsType
            document.querySelector("#recordList").url = targetURL
        }
    }

    function processUserSearch(textToSearch) {
        document.querySelector("#pageInfoPanel").innerHTML = "<g:message code="searchResultLbl"/> '" + textToSearch + "'"
        document.querySelector("#pageInfoPanel").style.display = "block"
        document.querySelector("#recordList").url = "${createLink(controller: 'transaction', action: 'index')}?searchText=" + textToSearch
    }

    function processUserSearchJSON(jsonData) {
        document.querySelector("#recordList").params = jsonData
        document.querySelector("#recordList").url = "${createLink(controller: 'transaction', action: 'index')}"
    }
</asset:script>