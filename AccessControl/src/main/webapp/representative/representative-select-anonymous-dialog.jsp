<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-select-anonymous-dialog">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click style="min-height: 250px; top: 10px; display: block;">
            <style>
                :host { display:block;}
            </style>
            <div>
                <div style="font-size: 1.1em;">
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:0px auto;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.saveAsRepresentativeLbl}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div hidden="{{!infoRequestStep}}" class="vertical layout center center-justified" style="margin:10px 0;">
                    <label>${msg.numWeeksAnonymousDelegationMsg}</label>
                    <input type="number" id="numWeeksAnonymousDelegation" min="1" value="" max="52" required
                           style="width:120px;margin:10px 20px 0px 7px;" class="form-control"
                           title="${msg.numWeeksAnonymousDelegationMsg}">
                </div>
                </div>
                <div hidden="{{!confirmStep}}" vertical layout center center-justified>
                    <div style="margin: 20px 0 10px 10px;">
                        <vs-html-echo id="delegationMsg"></vs-html-echo>
                    </div>
                    <div style="margin:25px 0 25px 0;">
                        <vs-html-echo html="{{anonymousDelegationMsg}}"></vs-html-echo>
                    </div>
                </div>
                <div hidden="{{!responseStep}}">
                    <p style="text-align: center; font-size: 1.2em;">
                        <span>{{anonymousDelegationResponseMsg}}</span>
                    </p>
                    <p style="text-align: center; font-size: 1.2em;">
                        ${msg.anonymousDelegationReceiptMsg}
                    </p>
                </div>
                <div hidden="{{responseStep}}">
                    <div class="layout horizontal" style="margin:0px 20px 0px 0px;  font-size:1.2em;">
                        <div class="flex"></div>
                        <div style="margin:10px 20px 10px 0px;">
                            <button on-click="cancel" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-times"></i> ${msg.cancelLbl}
                            </button>
                        </div>
                        <div style="margin:10px 0px 10px 0px;">
                            <button on-click="accept" style="margin: 0px 0px 0px 5px;">
                                <i class="fa fa-check"></i> ${msg.acceptLbl}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-select-anonymous-dialog',
            properties: {
                representative:{type:Object},
                infoRequestStep:{type:Boolean, value:true},
                confirmStep:{type:Boolean, value:false},
                responseStep:{type:Boolean, value:false},
                anonymousLbl:{type:String, value:"${msg.anonymousLbl}"},
                anonymousDelegationMsg:{type:String, value:"${msg.anonymousLbl}"},
                anonymousDelegationResponseMsg:{type:String},
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.representative = representative
                this.infoRequestStep = true
                this.confirmStep = false
                this.$.numWeeksAnonymousDelegation.value = ""
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                this.$.xDialog.opened = true
            },
            accept: function() {
                console.log("accept")
                if(this.confirmStep) {
                    if(!this.$.numWeeksAnonymousDelegation.validity.valid) {
                        this.messageToUser = '${msg.numberFieldLbl}'
                        return
                    }
                    var operationVS = new OperationVS(Operation.ANONYMOUS_REPRESENTATIVE_SELECTION)
                    operationVS.jsonStr = JSON.stringify({operation:Operation.ANONYMOUS_REPRESENTATIVE_SELECTION,
                            representative:this.representative,
                            weeksOperationActive:this.$.numWeeksAnonymousDelegation.value})
                    operationVS.signedMessageSubject = '${msg.representativeDelegationMsgSubject}'
                    operationVS.setCallback(function(appMessage) { this.delegationResponse(appMessage) }.bind(this))
                    VotingSystemClient.setMessage(operationVS);
                } else {
                    this.messageToUser = null
                    var msgTemplate = "${msg.selectRepresentativeConfirmMsg}";
                    if(!this.$.numWeeksAnonymousDelegation.validity.valid) {
                        showMessageVS('${msg.numWeeksAnonymousDelegationMsg}', '${msg.errorLbl}')
                        return
                    }
                    var weeksMsgTemplate = "${msg.numWeeksResultAnonymousDelegationMsg}";
                    this.$.delegationMsg.html = msgTemplate.format(this.anonymousLbl, this.representative.name)
                    this.anonymousDelegationMsg = weeksMsgTemplate.format(this.anonymousLbl, this.$.numWeeksAnonymousDelegation.value)
                    this.confirmStep = true
                    this.infoRequestStep = false
                }
            },
            delegationResponse:function(appMessage) {
                console.log(this.tagName + " - delegationResponse - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '${msg.operationERRORCaption}'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.responseStep = true
                    this.infoRequestStep = false
                    this.confirmStep = false
                    this.anonymousDelegationResponseMsg = "${msg.selectedRepresentativeMsg}".format(this.representativeFullName)
                    return
                } else if (ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                    caption = "${msg.anonymousDelegationActiveErrorCaption}"
                    msg = appMessageJSON.message + "<br/>" +
                            "${msg.downloadReceiptMsg}".format(appMessageJSON.URL)
                }
                showMessageVS(msg, caption)
                this.click() //hack to refresh screen
            },
            cancel: function() {
                if(this.infoRequestStep) this.close()
                else if(this.confirmStep) {
                    this.infoRequestStep = true
                    this.confirmStep = false
                }
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>
