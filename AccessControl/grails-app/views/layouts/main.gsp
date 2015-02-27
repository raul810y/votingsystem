<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="${assetPath(src: 'icon_16/fa-pie-chart.png')}" type="image/x-icon">
    <meta name="HandheldFriendly" content="true" />
    <meta name="viewport" content="width=device-width, height=device-height, user-scalable=no" />
    <meta name="mobile-web-app-capable" content="yes">
    <title><g:message code="appTitle"/></title>
    <asset:stylesheet src="votingSystem.css"/>
    <vs:webcss dir="font-awesome/css" file="font-awesome.min.css"/>
    <vs:webscript dir='webcomponentsjs' file="webcomponents.min.js"/>
    <vs:webresource dir="polymer" file="polymer.html"/>

    <asset:javascript src="utilsVS.js"/>
    <g:include view="/include/utils_js.gsp"/>
    <vs:webresource dir="font-roboto" file="roboto.html"/>
    <vs:webresource dir="vs-navbar" file="vs-navbar.html"/>
    <vs:webresource dir="core-ajax" file="core-ajax.html"/>
    <vs:webresource dir="paper-item" file="paper-item.html"/>
    <vs:webresource dir="core-signals" file="core-signals.html"/>
    <vs:webresource dir="paper-button" file="paper-button.html"/>
    <vs:webresource dir="vs-innerpage-signal" file="vs-innerpage-signal.html"/>
    <vs:webcomponent path="/element/alert-dialog"/>

    <!--<script type='text/javascript' src='http://getfirebug.com/releases/lite/1.2/firebug-lite-compressed.js'></script>-->
    <g:layoutHead/>
</head>
<body id="voting_system_page" style="margin:0px auto 0px auto;">
<polymer-element name="nav-bar" attributes="url loading">
    <template>
        <g:include view="/include/styles.gsp"/>
        <!--<core-ajax id="ajax" auto on-core-response="{{ajaxResponse}}" on-core-error="{{ajaxError}}" handleAs="document"></core-ajax>-->
        <core-xhr id="ajax" ></core-xhr>
        <!-- put core signals names in lower case !!!-->
        <core-signals on-core-signal-vs-innerpage="{{innerPageSignal}}"></core-signals>
        <vs-navbar id="_navbar" style="display: none;">
            <core-header-panel mode="seamed" id="core_header_panel" navigation flex class="vs-navbar">
                <core-toolbar id="core_toolbar" style="background-color: #ba0011;">
                </core-toolbar>
                <core-menu valueattr="label" id="core_menu" theme="core-light-theme" style="font-size: 1.2em;">
                    <core-selector id="coreSelector" selected="{{coreSelectorValue}}" valueattr="data-href" on-core-select="{{drawerItemSelected}}">
                        <g:if test="${"admin".equals(params.menu)}">
                            {{ "<g:message code="adminPageTitle"/>" | setTitle}}
                            <!--<template if="{{isClientToolConnected}}">-->
                                <paper-item data-href="${createLink(controller: 'eventVSElection')}">
                                    <i class="fa fa-envelope" style="margin:0px 10px 0px 0px;"></i> <g:message code="electionSystemLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'eventVSElection', action:'editor', absolute: true)}">
                                    <i class="fa fa-envelope" style="margin:0px 10px 0px 0px;"></i> <g:message code="publishVoteLbl"/>
                                </paper-item>

                                <!--<paper-item data-href="${createLink(controller: 'eventVSManifest')}">
                                    <i class="fa fa-file-text" style="margin:0px 10px 0px 0px;"></i> <g:message code="manifestSystemLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'eventVSManifest', action:'editor')}">
                                    <i class="fa fa-file-text" style="margin:0px 10px 0px 0px;"></i> <g:message code="publishManifestLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'eventVSClaim')}">
                                    <i class="fa fa-exclamation-triangle" style="margin:0px 10px 0px 0px;"></i> <g:message code="claimSystemLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'eventVSClaim', action:'editor')}">
                                    <i class="fa fa-exclamation-triangle" style="margin:0px 10px 0px 0px;"></i> <g:message code="publishClaimLbl"/>
                                </paper-item>-->
                                <paper-item data-href="${createLink(controller: 'representative')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="representativesPageLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'representative', action:'newRepresentative')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="newRepresentativeLbl"/>
                                </paper-item>
                                <paper-item data-href="${createLink(controller: 'representative', action:'edit')}">
                                    <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="editRepresentativeLbl"/>
                                </paper-item>
                            <!--</template>-->
                        </g:if>
                        <g:else>
                            {{ "<g:message code="votingsystemPageLbl"/>" | setTitle}}
                            <paper-item data-href="${createLink(controller: 'eventVSElection')}">
                                <i class="fa fa-envelope" style="margin:0px 10px 0px 0px;"></i> <g:message code="electionSystemLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'representative')}">
                                <i class="fa fa-hand-o-right" style="margin:0px 10px 0px 0px;"></i> <g:message code="representativesPageLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'app', action:'tools')}">
                                <i class="fa fa-cogs" style="margin:0px 10px 0px 0px;"></i> <g:message code="toolsLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'subscriptionVS', action:'feeds')}">
                                <i class="fa fa-rss" style="margin:0px 10px 0px 0px;"></i> <g:message code="subscriptionLbl"/>
                            </paper-item>
                            <paper-item data-href="${createLink(controller: 'app', action:'contact')}">
                                <i class="fa fa-phone" style="margin:0px 10px 0px 0px;"></i> <g:message code="contactLbl"/>
                            </paper-item>
                        </g:else>
                    </core-selector>
                </core-menu>
            </core-header-panel>
            <div id="appTitle" style="font-size:1.5em;width: 100%; text-align: center;" tool>{{appTitle}}</div>
            <content id="content"></content>
        </vs-navbar>
        <div hidden?="{{!loading}}" style="width: 30px;margin: 100px auto 0px auto;">
            <i class="fa fa-cog fa-spin" style="font-size:3em;color:#ba0011;"></i>
        </div>
        <content id="content"></content>
    </template>
    <script>
        Polymer('nav-bar', {
            appTitle:"<g:message code="appTitle"/>",
            url:'',
            ajaxOptions:{method:'get', responseType:'document'},
            ready: function() {
                this.$._navbar.searchVisible(false)
                this.$._navbar.style.display = 'block';
                this.fire('nav-bar-ready');
                var navBar = this
                window.addEventListener('popstate', function(event) {
                    navBar.url = document.location.href
                });
                this.isClientToolConnected = window['isClientToolConnected']
                console.log(this.tagName + " - ready - isClientToolConnected: " + this.isClientToolConnected)
            },
            innerPageSignal:function(e, detail, sender) {
                console.log("innerPageSignal - caption:" + detail.caption + " - url: " + detail.url)
                sendSignalVS(detail)
                var sufix = ""
                if('admin' === menuType) sufix = ' - <g:message code="adminLbl"/>'
                if('superuser' === menuType) sufix = ' - <g:message code="superUserLbl"/>'
                if(detail.caption) this.appTitle = detail.caption + sufix
                if(detail.searchVisible) this.$._navbar.searchVisible(detail.searchVisible)
                if(detail.url) this.loadURL(detail.url)
                document.dispatchEvent( new Event('innerPageSignal'));
            },
            urlChanged: function() {
                this.loadURL(this.url)
            },
            loadURL: function(urlToLoad) {
                this.loading= true;
                history.pushState(null, null, urlToLoad);
                var newURL = updateMenuLink(urlToLoad, "mode=innerPage")
                this.ajaxOptions.url = newURL
                this.ajaxOptions.callback = this.ajaxResponse.bind(this)
                this.$.ajax.request(this.ajaxOptions)
                /*if(this.$.ajax.url == newURL)  this.$.ajax.go()
                else this.$.ajax.url = newURL*/
            },
            drawerItemSelected: function(e) {
                if(e.detail.isSelected) {
                    this.fire('item-selected', this.coreSelectorValue)
                    if(this.$.coreSelector.selectedItem != null && 'changeToAdmin' == this.$.coreSelector.selectedItem.id) {
                        window.location.href = window.location.href.replace("menu=superuser", "menu=admin");
                    } else {
                        this.loadURL(this.coreSelectorValue)
                    }
                    this.coreSelectorValue = null
                }
            },
            searchVisible: function(isVisible) {
                this.$._navbar.searchVisible(isVisible)
            },
            setTitle: function(appTitle) {
                this.appTitle = appTitle
            },
            ajaxResponse: function(ajaxDocument, xhr) {
                if(400 === xhr.status || 404 === xhr.status) {
                    this.loading = false
                    alert(ajaxDocument.body.innerHTML)
                    return
                }
                var links = ajaxDocument.querySelectorAll('link')
                var numImports = 0
                for (var i = 0; i < links.length; i++) {
                    console.log("links[i].innerHTML: " + links[i].href + " - rel: " + links[i].rel)
                    if('import' == links[i].rel) {
                        ++numImports
                        if(i == (links.length - 1)) {
                            links[i].onload = function() { this.loading = false; }.bind(this);
                        }
                        document.head.appendChild(links[i]);
                    }
                }
                if(numImports == 0) document.querySelector('#navBar').loading = false;
                for (var i = 0; i < ajaxDocument.scripts.length; i++) {
                    var script = document.createElement("script");
                    script.innerHTML = ajaxDocument.scripts[i].innerHTML;
                    console.log("script.src: " + script.src)
                    document.head.appendChild(script);
                }
                this.innerHTML = ajaxDocument.body.innerHTML
                updateLinksVS(document.querySelectorAll("#navBar a"))
            },
            ajaxError: function(e) {
                console.log(this.tagName + " - ajaxError")
                if(ResponseVS.SC_PRECONDITION_FAILED == e.detail.xhr.status) {
                    this.loading = false
                    var response = e.detail.xhr.responseText
                    showMessageVS(response, '<g:message code="errorLbl"/>')
                }
            }
        });
    </script>
</polymer-element>
<nav-bar id="navBar">
    <g:layoutBody/>
</nav-bar>
<alert-dialog id="_votingsystemMessageDialog"></alert-dialog>
<core-signals id="coreSignals"></core-signals>
</body>
</html>
<asset:script>
    window.addEventListener('WebComponentsReady', function(e) {});
    document.querySelector('#coreSignals').addEventListener('core-signal-vs-innerpage', function(e) {});
    document.querySelector('#navBar').addEventListener('nav-bar-ready', function(e) {});

    document.addEventListener('polymer-ready', function() {
        console.log("main.gsp - polymer-ready")
        updateLinksVS(document.getElementsByTagName('a'))
    });
</asset:script>
<asset:deferredScripts/>