<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webresource dir="core-signals" file="core-signals.html"/>
<vs:webresource dir="core-animated-pages" file="core-animated-pages.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="core-item" file="core-item.html"/>
<vs:webresource dir="core-selector" file="core-selector.html"/>
<vs:webresource dir="paper-dropdown-menu" file="paper-dropdown-menu.html"/>
<vs:webresource dir="paper-dropdown" file="paper-dropdown.html"/>
<vs:webresource dir="core-menu" file="core-menu.html"/>
<vs:webcomponent path="/transactionVS/transactionvs-form"/>
<vs:webcomponent path="/userVS/uservs-list"/>
<vs:webcomponent path="/groupVS/groupvs-user"/>

<polymer-element name="groupvs-details" attributes="selectedItem subpage">
<template>
    <style shim-shadowdom>
        body /deep/ paper-dropdown-menu.narrow { max-width: 200px; width: 300px; }
        .optionsIcon {margin:0 5px 0 2px; color:#6c0404;}
        .colored { color: #6c0404; }
    </style>
    <g:include view="/include/styles.gsp"/>
    <core-signals on-core-signal-messagedialog-accept="{{messagedialogAccepted}}" on-core-signal-messagedialog-closed="{{messagedialogClosed}}"
                  on-core-signal-uservs-selected="{{showUserDetails}}" ></core-signals>
    <core-animated-pages id="pages" flex selected="{{page}}" on-core-animated-pages-transition-end="{{transitionend}}"
         transitions="cross-fade-all">
    <section id="page1">
    <div cross-fade style="max-width: 900px; margin:0 auto;">
        <div vertical layout flex>
            <div id="messagePanel" class="messagePanel messageContent text-center" style="font-size: 1.4em;display:none;">
            </div>
            <div style="display:{{isAdminView && isClientToolConnected? 'block':'none'}}">
                <div layout horizontal center center-justified style="margin:0 0 20px 0;">
                    <div layout horizontal center center-justified>
                        <i class="fa fa-cogs optionsIcon"></i>
                        <paper-dropdown-menu halign="right" style="width: 200px;"
                                     label="<g:message code="configGroupvsLbl"/>" on-core-select="{{configGroup}}">
                            <paper-dropdown class="dropdown colored" transition="">
                                <core-menu id="configGroupCoreMenu">
                                    <paper-item id="editDataItem"><g:message code="editDataLbl"/></paper-item>
                                    <paper-item id="cancelGroupVSItem"><g:message code="cancelGroupVSLbl"/></paper-item>
                                </core-menu>
                            </paper-dropdown>
                        </paper-dropdown-menu>
                    </div>

                    <div layout horizontal center center-justified style="margin:0 0 0 60px;">
                        <i class="fa fa-money optionsIcon"></i>
                        <paper-dropdown-menu label="<g:message code="makeTransactionVSFromGroupVSLbl"/>"
                                 on-core-select="{{showTransactionVSDialog}}" style="width: 300px;">
                            <paper-dropdown class="dropdown colored"  transition="">
                                <core-menu id="transactionvsCoreMenu">
                                    <paper-item id="fromGroupToMember"><g:message code="makeTransactionVSFromGroupVSToMemberLbl"/></paper-item>
                                    <paper-item id="fromGroupToMemberGroup"><g:message code="makeTransactionVSFromGroupVSToMemberGroupLbl"/></paper-item>
                                    <paper-item id="fromGroupToAllMember"><g:message code="makeTransactionVSFromGroupVSToAllMembersLbl"/></paper-item>
                                </core-menu>
                            </paper-dropdown>
                        </paper-dropdown-menu>
                    </div>
                </div>
            </div>

            <template if="{{isUserView}}">
                <div layout horizontal center center-justified style="margin:0 0 10px 10px; font-size: 0.9em;">
                    <paper-button raised style="margin:0 20px 0 0;" on-click="{{subscribeToGroup}}">
                        <i class="fa fa-sign-in"></i> <g:message code="subscribeGroupVSLbl"/>
                    </paper-button raised>
                    <paper-button raised style="margin:0 20px 0 0;" on-click="{{makeTransactionVS}}">
                        <i class="fa fa-money"></i> <g:message code="makeTransactionVSLbl"/>
                    </paper-button raised>
                </div>
            </template>
            <div id="pageHeader" layout horizontal center center-justified>
                <div horizontal layout flex style="padding:7px 0px 0px 7px;">
                    <template if="{{subpage}}">
                        <div style="margin: 10px 20px 10px 0;" title="<g:message code="backLbl"/>" >
                            <paper-fab mini icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                        </div>
                    </template>
                    <template if="{{groupvs.userVS.tags.length > 0}}">
                        <div layout horizontal center center-justified>
                            <i class="fa fa-tag" style="color:#888; margin: 0 10px 0 0;"></i>
                            <template repeat="{{tag in groupvs.userVS.tags}}">
                                <a class="btn btn-default" style="font-size: 0.7em;
                                margin:0px 5px 0px 0px;padding:3px;">{{tag.name}}</a>
                            </template>
                        </div>
                    </template>
                </div>
                <div style="font-size: 1.5em; margin:5px 0 0 0;font-weight: bold; color:#6c0404;">
                    <div style="text-align: center;" groupvsId-data="{{groupvs.userVS.id}}">{{groupvs.userVS.name}}</div>
                </div>
                <div flex style="margin:5px 10px 0 0; font-size: 0.7em; color:#888; text-align: right; vertical-align: bottom;">
                    <b><g:message code="IBANLbl"/>: </b>{{groupvs.userVS.IBAN}}
                </div>
            </div>
        </div>
        <div class="eventContentDiv" style="">
            <vs-html-echo html="{{groupvs.userVS.description}}"></vs-html-echo>
        </div>

        <div layout horizontal>
            <div class="linkVS" style="margin: 5px 0 0 0;" on-click="{{goToWeekBalance}}">
                <i class="fa fa-bar-chart"></i> <g:message code="goToWeekBalanceLbl"/>
            </div>
            <div flex></div>
            <div style="margin:5px 10px 0 0; font-size: 0.9em; color:#888;">
                <b><g:message code="representativeLbl"/>: </b>{{groupvs.userVS.representative.firstName}} {{groupvs.userVS.representative.lastName}}
            </div>
        </div>

        <div style="min-height: 300px; border-top: 1px solid #ccc; margin: 15px 0 0 0; padding: 10px 0 0 0;">
            <div  style="text-align:center; font-size: 1.3em;font-weight: bold; color: #888;margin: 0 0 10px 0;
                text-decoration: underline;">
                <g:message code="usersLbl"/>
            </div>

            <div id="userList" style="margin: 0 0 100px 0;">
                <uservs-list id="userList" menuType="${params.menu}"></uservs-list>
            </div>

        </div>
        <template if="{{!isClientToolConnected}}">
            <div id="clientToolMsg" class="text-center" style="color:#6c0404; font-size: 1.2em;margin:30px 0 0 0;">
                <g:message code="clientToolNeededMsg"
                           args="${["${grailsApplication.config.grails.serverURL}/tools/ClientTool.zip"]}"/>.
                <g:message code="clientToolDownloadMsg" args="${[createLink( controller:'app', action:'tools')]}"/>
            </div>
        </template>
    </div>
    </section>

    <section id="page2">
        <div cross-fade>
            <transactionvs-form id="transactionvsForm" subpage></transactionvs-form>
        </div>
    </section>
    </core-animated-pages>

    <groupvs-user id="userDescription"></groupvs-user>
</template>
<script>
    Polymer('groupvs-details', {
        isSelected: false,
        subpage:false,
        publish: { groupvs: {} },
        isClientToolConnected:false,
        ready : function() {
            console.log(this.tagName + " - ready - subpage: " + this.subpage)
            //this.isClientToolConnected = window['isClientToolConnected']
            this.isClientToolConnected = true
            this.$.transactionvsForm.addEventListener('operation-finished', function (e) {
                this.page = 0;
            }.bind(this))
        },
        makeTransactionVS:function() {
            console.log(this.tagName + " - makeTransactionVS")
            this.$.transactionvsForm.init(Operation.FROM_USERVS, this.groupvs.userVS.name, this.groupvs.userVS.IBAN ,
                    this.groupvs.userVS.id)
            this.page = 1;
        },
        messagedialogAccepted:function(e, detail, sender) {
            console.log(this.tagName + ".messagedialogAccepted")
            if('cancel_group' == detail.callerId) {
                var webAppMessage = new WebAppMessage(Operation.COOIN_GROUP_CANCEL)
                webAppMessage.serviceURL = "${createLink(controller:'groupVS', action:'cancel',absolute:true)}/" + this.groupvs.userVS.id
                webAppMessage.signedMessageSubject = "<g:message code="cancelGroupVSSignedMessageSubject"/>"
                webAppMessage.signedContent = {operation:Operation.COOIN_GROUP_CANCEL, groupvsName:this.groupvs.userVS.name,
                    id:this.groupvs.userVS.id}
                webAppMessage.contentType = 'application/pkcs7-signature'
                webAppMessage.setCallback(function(appMessage) {
                    this.appMessageJSON = JSON.parse(appMessage)
                    var caption = '<g:message code="groupCancelERRORLbl"/>'
                    if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                        caption = "<g:message code='groupCancelOKLbl'/>"
                        loadURL_VS("${createLink(controller:'groupVS', action:'',absolute:true)}/" + this.groupvs.userVS.id)
                    }
                    showMessageVS(this.appMessageJSON.message, caption, this.tagName)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
                this.appMessageJSON = null
            }
        },
        goToWeekBalance:function() {
            loadURL_VS("${createLink( controller:'balance', action:"userVS", absolute:true)}/" + this.groupvs.userVS.id)
        },
        messagedialogClosed:function(e) {
            console.log("messagedialog signal - messagedialogClosed: " + e)
            if(this.tagName == e) {
                if(this.appMessageJSON != null && ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                    window.location.href = updateMenuLink(this.appMessageJSON.URL)
                }
            }
        },
        subscribeToGroup: function () {
            console.log("subscribeToGroup")
            var groupvsRepresentative = {id:this.groupvs.userVS.representative.id, nif:this.groupvs.userVS.representative.nif}
            var groupVSData = {id:this.groupvs.userVS.id, name:this.groupvs.userVS.name , representative:groupvsRepresentative}
            var webAppMessage = new WebAppMessage(Operation.COOIN_GROUP_SUBSCRIBE)
            webAppMessage.serviceURL = "${createLink( controller:'groupVS', absolute:true)}/" + this.groupvs.userVS.id + "/subscribe"
            webAppMessage.signedMessageSubject = "<g:message code="subscribeToCooinGroupMsg"/>"
            webAppMessage.signedContent = {operation:Operation.COOIN_GROUP_SUBSCRIBE, groupvs:groupVSData}
            webAppMessage.contentType = 'application/pkcs7-signature'
            webAppMessage.setCallback(function(appMessage) {
                console.log("subscribeToGroupCallback - message: " + appMessage);
                var appMessageJSON = JSON.parse(appMessage)
                var caption
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "<g:message code='groupSubscriptionOKLbl'/>"
                    loadURL_VS( "${createLink( controller:'groupVS', absolute:true)}/" + this.groupvs.userVS.id)
                } else caption = '<g:message code="groupSubscriptionERRORLbl"/>'
                var msg = appMessageJSON.message
                showMessageVS(msg, caption)
            }.bind(this))
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        },
        groupvsChanged:function() {
            console.log("groupvsChanged - groupvs: " + Object.prototype.toString.call(this.groupvs))
            if(("admin" == menuType || "superuser" == menuType) && 'ACTIVE' == this.groupvs.userVS.state) this.isAdminView = true
            else {
                this.isAdminView = false
                if("user" == menuType && 'ACTIVE' == this.groupvs.userVS.state) this.isUserView = true
                else this.isUserView = false
            }
            if('ACTIVE' == this.groupvs.userVS.state) {
                this.$.messagePanel.style.display = 'none'
            } else if('PENDING' == this.groupvs.userVS.state) {
                this.$.pageHeader.style.color = "#fba131"
                this.$.messagePanel.classList.add("groupvsPendingBox");
                this.$.messagePanel.innerHTML = "<g:message code="groupvsPendingLbl"/>"
                this.$.messagePanel.style.display = 'block'
            } else if('CANCELLED' == this.groupvs.userVS.state) {
                this.$.pageHeader.style.color = "#6c0404"
                this.$.messagePanel.classList.add("groupvsClosedBox");
                this.$.messagePanel.innerHTML = "<g:message code="groupvsClosedLbl"/>"
                this.$.messagePanel.style.display = 'block'
                this.isAdminView = false
            }
            this.$.userList.url = "${createLink(controller: 'groupVS', action:'')}/" + this.groupvs.userVS.id + "/users"
            this.fire('core-signal', {name: "vs-innerpage", data: {caption:"<g:message code="groupvsLbl"/>"}});
            console.log("this.isUserView: " + this.isUserView + " - groupvs.userVS.state: " + this.groupvs.userVS.state +
                " - menuType: " + menuType)
        },
        configGroup:function(e, details) {
            //e.detail.isSelected = false
            if('cancelGroupVSItem' == e.detail.item.id) {
                showMessageVS("<g:message code="cancelGroupVSDialogMsg"/>".format(this.groupvs.userVS.name),
                        "<g:message code="confirmOperationMsg"/>", 'cancel_group', true)
            } else if('editDataItem' == e.detail.item.id) {
                window.location.href = "${createLink( controller:'groupVS', action:'edit', absolute:true)}?menu=admin&id=" +
                        this.groupvs.userVS.id
            }
            this.$.configGroupCoreMenu.selected = null
        },
        showTransactionVSDialog:function(e) {
            console.log("showTransactionVSDialog")
            //e.detail.isSelected
            if('fromGroupToMember' == e.detail.item.id) {
                this.$.transactionvsForm.init(Operation.FROM_GROUP_TO_MEMBER, this.groupvs.userVS.name,
                        this.groupvs.userVS.IBAN , this.groupvs.userVS.id)
            } else if('fromGroupToMemberGroup' == e.detail.item.id) {
                this.$.transactionvsForm.init(Operation.FROM_GROUP_TO_MEMBER_GROUP, this.groupvs.userVS.name,
                        this.groupvs.userVS.IBAN, this.groupvs.userVS.id)
            } else if('fromGroupToAllMember' == e.detail.item.id) {
                this.$.transactionvsForm.init(Operation.FROM_GROUP_TO_ALL_MEMBERS, this.groupvs.userVS.name,
                        this.groupvs.userVS.IBAN, this.groupvs.userVS.id)
            }
            this.page = 1;
            this.$.transactionvsCoreMenu.selected = null
        },
        back:function() {
            this.fire('core-signal', {name: "groupvs-details-closed", data: this.groupvs.userVS.id});
        },
        showUserDetails:function(e, detail, sender) {
            console.log(this.tagName + " - showUserDetails")
            this.$.userDescription.show("${createLink(controller: 'groupVS')}/" + this.groupvs.userVS.id + "/user", detail)
        }
    })
</script>
</polymer-element>