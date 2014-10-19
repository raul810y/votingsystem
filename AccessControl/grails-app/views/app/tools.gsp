<!DOCTYPE html>
<html>
<head>
    <g:render template="/template/pagevs"/>
</head>
<body>
<vs-innerpage-signal title="<g:message code="toolsPageTitle"/>"></vs-innerpage-signal>
<div class="pageContentDiv">
    <g:if test="${request.getHeader("user-agent").toLowerCase().contains('android')}">
        <div class="userAdvert">
            <ul>
                <li><g:message code="androidAppNeededMsg"/></li>
                <li><g:message code="androidAppDownloadMsg"  args="${[grailsApplication.config.grails.serverURL + "/android/SistemaVotacion.apk"]}"/></li>
                <li><g:message code="androidCertInstalledMsg"/></li>
                <li><g:message code="androidSelectAppMsg"/></li>
            </ul>
        </div>
    </g:if>
    <g:else>
        <div><g:message code="clientToolNeededMsg"/></div>
        <div><g:message code="javaRequirementsMsg"
                        args="${["http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk"]}"/></div>
        <a href="${grailsApplication.config.grails.serverURL}/tools/ClientTool.zip" class=""
           style="margin:40px 20px 0px 0px; width:400px;">
            <g:message code="downloadClientToolAppLbl"/> <i class="fa fa-cogs"></i></a>
    </g:else>
</div>

</body>
</html>
<asset:script>
</asset:script>

