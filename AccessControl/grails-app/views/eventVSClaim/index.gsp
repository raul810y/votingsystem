<html>
<head>
    <g:render template="/template/pagevs"/>
    <vs:webresource dir="vs-advanced-search-dialog" file="vs-advanced-search-dialog.html"/>
    <vs:webcomponent path="/element/search-info"/>
    <vs:webcomponent path="/eventVSClaim/eventvs-claim-list"/>
</head>
<body>
    <vs-innerpage-signal caption="<g:message code="claimSystemLbl"/>"></vs-innerpage-signal>
    <div class="pageContentDiv">
        <search-info id="searchInfo"></search-info>
        <p id="pageInfoPanel" class="text-center" style="margin: 20px auto 20px auto; font-size: 1.3em;
        background-color: #f9f9f9; max-width: 1000px; padding: 10px; display: none;"></p>
        <eventvs-claim-list id="eventvsList" url="${createLink(controller: 'eventVSClaim', action: 'index')}?menu=${params.menu}&eventVSState=ACTIVE"
                      eventvstype="claim"></eventvs-claim-list>
    </div>
    <vs-advanced-search-dialog id="advancedSearchDialog"></vs-advanced-search-dialog>
</body>
</html>
<asset:script>
    function processSearch(textToSearch, dateBeginFrom, dateBeginTo) {
        var ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}?searchText=" +
            textToSearch + "&dateBeginFrom=" + dateBeginFrom + "&dateBeginTo=" + dateBeginTo + "&eventvsType=CLAIM"
    }

    function processSearchJSON(jsonData) {
        var ajaxUrl= "${createLink(controller: 'search', action: 'eventVS')}";
    }
</asset:script>