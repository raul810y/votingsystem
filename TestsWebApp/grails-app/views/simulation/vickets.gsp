<!DOCTYPE html>
<html>
<head>
    <g:if test="${'simplePage'.equals(params.mode)}"><meta name="layout" content="simplePage" /></g:if>
    <g:elseif test="${'innerPage'.equals(params.mode)}"></g:elseif>
    <g:else><meta name="layout" content="main" /></g:else>
    <title><g:message code="simulationWebAppCaption"/></title>
</head>
<body>
    <div layout vertical class="pageContentDiv" style="margin: 0px auto 0px auto;padding:0px 30px 0px 30px;">
        <div  style="">
            <ol class="breadcrumbVS pull-left">
                <li><a href="${grailsApplication.config.grails.serverURL}"><g:message code="homeLbl"/></a></li>
                <li class="active"><g:message code="vicketsOperationsLbl"/></li>
            </ol>
        </div>
        <div class="text-center" style="margin: 50px 0 20 0; font-weight: bold; font-size: 2em; color: #6c0404;">
            <g:message code="vicketsOperationsLbl"/>
        </div>
        <div>
            <a id="initUserBaseDataButton" href="${createLink(controller: 'vicket', action:'initUserBaseData', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="initUserBaseDataButton"/>
            </a>
            <a id="makeDepositButton" href="${createLink(controller: 'vicket', action:'deposit', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="makeDepositButton"/>
            </a>
            <a href="${createLink(controller: 'vicket', action:'addUsersToGroup', absolute:true)}"
               class="btn btn-default btn-lg" role="button" style="margin:15px 20px 0px 0px; width:275px;">
                <g:message code="addUsersToGroupButton"/>
            </a>
        </div>
    </div>
</body>
<asset:script>
    function openWindow(targetURL) {
        var width = 1000
        var height = 800
        var left = (screen.width/2) - (width/2);
        var top = (screen.height/2) - (height/2);
        var title = ''

        var newWindow =  window.open(targetURL, title, 'toolbar=no, scrollbars=yes, resizable=yes, '  +
                'width='+ width +
                ', height='+ height  +', top='+ top +', left='+ left + '');
    }
</asset:script>
</html>