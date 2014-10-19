<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <asset:stylesheet src="vickets.css"/>
    <link rel="stylesheet" href="${resource(dir: 'bower_components/font-awesome/css', file: 'font-awesome.min.css')}" type="text/css"/>
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/core-signals', file: 'core-signals.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/vs-innerpage-signal', file: 'vs-innerpage-signal.html')}">
    <link rel="import" href="<g:createLink  controller="element" params="[element: '/element/alert-dialog.gsp']"/>">
    <link rel="import" href="${resource(dir: '/bower_components/vs-button', file: 'vs-button.html')}">
    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body id="voting_system_page" style="margin:0px auto 0px auto; max-width: 1200px;">
    <div id="appTitle" style="font-size:1.5em;width: 100%; text-align: center; margin:15px auto;"></div>
    <div id="pageLayoutDiv" style="display:none;">
        <g:layoutBody/>
    </div>
    <div id="loadingDiv" style="width: 30px;margin: 100px auto 0px auto">
        <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
    </div>

    <div layout horizontal center center-justified style="position:absolute; top:100px; width:1200px;">
        <div>
            <alert-dialog id="_votingsystemMessageDialog"></alert-dialog>
        </div>
    </div>
    <core-signals id="coreSignals"></core-signals>
</body>
</html>
<asset:script>
    document.addEventListener('polymer-ready', function() {
        document.querySelector('#pageLayoutDiv').style.display = 'block';
        document.querySelector('#loadingDiv').style.display = 'none';
        updateMenuLinks()
    });

    document.querySelector('#coreSignals').addEventListener('core-signal-votingsystem-innerpage', function(e) {
        if(e.detail.title) document.querySelector('#appTitle').innerHTML = e.detail.title
        document.dispatchEvent( new Event('innerPageSignal'));
    });

</asset:script>
<asset:deferredScripts/>