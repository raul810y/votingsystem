<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/reason-dialog.vsp" rel="import"/>

<dom-module name="groupvs-user">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click style="min-height: 300px;">
            <iron-ajax id="ajax" url="{{url}}" last-response="{{subscriptionDto}}" handle-as="json" content-type="application/json"></iron-ajax>
            <div style="height: 400px;">

                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{subscriptionDto.uservs.name}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <div class="horizontal layout" style="font-size: 0.8em; margin: 5px 0 10px 0;">
                        <div style="font-weight: bold;color:#888;" class="flex">NIF: <span>{{subscriptionDto.uservs.nif}}</span></div>
                        <div hidden="{{!subscriptionDto.uservs.iban}}" style="font-weight: bold;color:#888;">
                            IBAN: <span>{{subscriptionDto.uservs.iban}}</span></div>
                    </div>
                    <div id="contentDiv">${msg.subscriptionRequestDateLbl}:
                        <span id="dateCreatedDiv">{{getDate(subscriptionDto.dateCreated)}}</span></div>
                </div>
                <div class="horizontal layout center center-justified" style="margin:10px 0 30px 0;">
                    <div hidden="{{!isClientToolConnected}}">
                        <div hidden="{{isActive}}">
                            <button hidden="{{!isAdmin}}" on-click="activateUser">
                                <i class="fa fa-thumbs-o-up"></i> ${msg.activateUserLbl}
                            </button>
                        </div>
                        <div hidden="{{!isActive}}">
                            <button hidden="{{!isAdmin}}" on-click="initCancellation">
                                <i class="fa fa-thumbs-o-down"></i> ${msg.deActivateUserLbl}
                            </button>
                        </div>
                    </div>
                    <button class="btnvs" on-click="goToUserVSPage">
                        <i class="fa fa-user"></i> ${msg.userVSPageLbl}
                    </button>
                </div>
                <div id="receipt" style="display:none;"> </div>
            </div>
            <div style="position: absolute; width: 100%; top:0px;left:0px;">
                <div class="layout horizontal center center-justified" style="padding:0px 0px 0px 0px;margin:0px auto 0px auto;">
                    <reason-dialog id="reasonDialog" caption="${msg.cancelSubscriptionFormCaption}" ></reason-dialog>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'groupvs-user',
            properties: {
                subscriptionDto: {type:Object, observer:'subscriptionDtoChanged'},
                isClientToolConnected: {type:Boolean},
                isActive: {type:Boolean},
                isAdmin: {type:Boolean}
            },
            ready:function(e) {
                this.isClientToolConnected = window['isClientToolConnected']
                document.querySelector("#voting_system_page").addEventListener('votingsystem-client-connected',
                        function() {  this.isClientToolConnected = true }.bind(this))
                this.$.reasonDialog.addEventListener('on-submit', function (e) {
                    console.log("deActivateUser")
                    var operationVS = new OperationVS(Operation.CURRENCY_GROUP_USER_DEACTIVATE)
                    operationVS.serviceURL = restURL + "/groupVS/deActivateUser"
                    operationVS.signedMessageSubject = "${msg.deActivateGroupUserMessageSubject}" + " '" + this.subscriptionDto.groupvs.name + "'"
                    operationVS.jsonStr = JSON.stringify({groupvsId:this.subscriptionDto.groupvs.id,
                        groupvsName:this.subscriptionDto.groupvs.name, userVSName:this.subscriptionDto.uservs.name,
                        userVSNIF:this.subscriptionDto.uservs.nif, reason:e.detail})
                    operationVS.setCallback(function(appMessage) {

                    }.bind(this))
                    VotingSystemClient.setMessage(operationVS);
                }.bind(this))
            },
            deActivateResponse:function(appMessage) {
                console.log(this.tagName + " - deActivateResponse - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '${msg.deActivateUserERRORLbl}'
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.deActivateUserOKLbl}"
                    this.fire('iron-signal', {name: "refresh-uservs-list", data: {uservs: this.userId}});
                    this.close()
                }
                showMessageVS(appMessageJSON.message, caption)
                this.click() //hack to refresh screen
            },
            getDate:function(timStamp) {
                return new Date(timStamp).getDayWeekAndHourFormat()
            },
            goToUserVSPage:function() {
                page.show(restURL + "/userVS/id/" + this.userId)
                this.$.xDialog.opened = false
            },
            openedChanged:function() {
                this.async(function() { this.$.xDialog.opened = this.opened});
            },
            subscriptionDtoChanged:function() {
                console.log(this.tagName + "subscriptionDtoChanged - subscriptionDto: " + this.subscriptionDto)
                this.isActive = false
                this.isPending = false
                this.isCancelled = false
                this.isAdmin = false
                if('ACTIVE' == this.subscriptionDto.state) {
                    this.isActive = true
                    this.caption = "${msg.userStateActiveLbl}"
                } else if('PENDING' == this.subscriptionDto.state) {
                    this.isPending = true
                    this.caption = "${msg.userStatePendingLbl}"
                } else if('CANCELED' == this.subscriptionDto.state) {
                    this.isCancelled = true
                    this.caption = "${msg.userStateCancelledLbl}"
                }
                this.isAdmin = ('admin' === menuType || 'superuser' === menuType)
            },
            show:function(baseURL, userId) {
                if(baseURL && userId) {
                    this.subscriptionDataURLPrefix = baseURL
                    this.userId = userId
                    this.$.ajax.url = this.subscriptionDataURLPrefix + "/" + this.userId
                    this.$.ajax.generateRequest()
                }
                this.$.xDialog.opened = true
            },
            activateUser : function(e) {
                console.log("activateUser")
                var operationVS = new OperationVS(Operation.CURRENCY_GROUP_USER_ACTIVATE)
                operationVS.serviceURL = restURL + "/groupVS/activateUser"
                operationVS.signedMessageSubject = "${msg.activateGroupUserMessageSubject}" + " '" +
                        this.subscriptionDto.groupvs.name + "'"
                operationVS.jsonStr = JSON.stringify({operation:Operation.CURRENCY_GROUP_USER_ACTIVATE,
                    groupvsId:this.subscriptionDto.groupvs.id, groupvsName:this.subscriptionDto.groupvs.name,
                    userVSName:this.subscriptionDto.uservs.name, userVSNIF:this.subscriptionDto.uservs.nif})
                operationVS.setCallback(function(appMessage) { this.activateResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            activateResponse: function(appMessage) {
                console.log(this.tagName + " - activateResponse - message: " + appMessage)
                var appMessageJSON = toJSON(appMessage)
                if(appMessageJSON != null) {
                    var caption = '${msg.activateUserERRORLbl}'
                    if (ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "${msg.activateUserOKLbl}"
                        this.fire('iron-signal', {name: "refresh-uservs-list", data: {uservs: this.userId}})
                        this.close()
                    }
                    showMessageVS(appMessageJSON.message, caption)
                }
            },
            initCancellation : function(e) {
                this.$.reasonDialog.show("${msg.cancelSubscriptionFormMsg}");
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>