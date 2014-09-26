<!DOCTYPE html>
<html>
<head>
    <title></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes">
    <script src="${resource(dir: '/bower_components/platform', file: 'platform.js')}"> </script>
    <link rel="import" href="${resource(dir: '/bower_components/font-roboto', file: 'roboto.html')}">
    <link rel="import" href="${resource(dir: '/bower_components/paper-tabs', file: 'paper-tabs.html')}">
    <style>
    html,body {
        height: 100%;
        margin: 0;
        font-family: 'RobotoDraft', sans-serif;
    }
    .headerTitle {
        font-size: 26px;
        font-weight:bold;
        line-height: 1;
        display:block;
        margin: 10px auto 0px auto;
        color:#ba0011;
        text-decoration: none;
    }
    </style>
</head>
<body >
<h3 style="text-align: center;">
    <a class="headerTitle" href="${grailsApplication.config.grails.serverURL}">${message(code: 'appTitle', null)}</a>
</h3>
<polymer-element name="info-page-tabs">
    <template>
        <style shim-shadowdom>
        .tabContent {
            padding: 10px 20px 10px 20px;
            margin:0px auto 0px auto;
            width:auto;
        }
        paper-tabs.transparent-teal {
            background-color: transparent;
            color:#ba0011;
            box-shadow: none;
            cursor: pointer;
        }

        paper-tabs.transparent-teal::shadow #selectionBar {
            background-color: #ba0011;
        }

        paper-tabs.transparent-teal paper-tab::shadow #ink {
            color: #ba0011;
        }
        </style>
        <div  style="width: 1000px; margin:0px auto 0px auto;">
            <paper-tabs  style="width: 1000px;margin:0px auto 0px auto;" class="transparent-teal center" valueattr="name"
                         selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                <paper-tab name="info" style="width: 400px"><g:message code="infoLbl"/></paper-tab>
                <paper-tab name="serviceList"><g:message code="serviceListLbl"/></paper-tab>
                <paper-tab name="appData"><g:message code="appDataLabel"/></paper-tab>
            </paper-tabs>
            <div id="infoDiv" class="tabContent" style="display:{{selectedTab == 'info'?'block':'none'}}">
                <div class="mainLink"><a href="http://www.sistemavotacion.org"><g:message code="webSiteLbl"/></a></div>
                <div class="mainLink"><a href="https://github.com/votingsystem/votingsystem/tree/master/Vickets">
                    <g:message code="sourceCodeLbl"/></a>
                </div>
            </div>

            <div id="serviceList" class="tabContent" style="display:{{selectedTab == 'serviceList'?'block':'none'}}">
                <g:include controller="serverInfo" action="serviceList" />
            </div>

            <div id="appData" class="tabContent" style="display:{{selectedTab == 'appData'?'block':'none'}}">
                <g:include controller="serverInfo" action="appData" />
            </div>
        </div>
    </template>

    <script>
        Polymer('info-page-tabs', {
            selectedTab:'info'
        });
    </script>
</polymer-element>
<info-page-tabs style="width: 1000px;"></info-page-tabs>
</body>
</html>