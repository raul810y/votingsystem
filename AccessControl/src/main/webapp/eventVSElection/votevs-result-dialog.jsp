<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="votevs-result-dialog">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click>
            <style></style>
            <iron-signals on-iron-signal-messagedialog-accept="cancellationConfirmed"
                          on-iron-signal-messagedialog-closed="confirmDialogClosed"></iron-signals>
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div hidden="{{!caption}}" style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px;
                        display:block;word-wrap:break-word;">
                    <vs-html-echo html="{{message}}"></vs-html-echo>
                </div>
                <div hidden="{{!isVoteResult}}">
                    <div hidden="{{!optionSelected}}">
                        <p style="text-align: center;">
                            ${msg.confirmOptionDialogMsg}:</p>
                        <div style="font-size: 1.2em; text-align: center;"><b>{{optionSelected}}</b></div>
                    </div>
                    <div hidden="{{!isOK}}">
                        <div class="layout horizontal" style="margin:15px 0 0 0;">
                            <div style="margin:10px 0px 10px 0px;">
                                <button on-click="checkReceipt">
                                    <i class="fa fa-certificate"></i><span>{{checkSignatureButtonMsg}}</span>
                                </button>
                            </div>
                            <div class="flex"></div>
                            <div style="margin:10px 0px 10px 0px;">
                                <button on-click="cancelVote" style="margin: 0px 0px 0px 5px;">
                                    <i class="fa fa-times"></i> ${msg.cancelVoteLbl}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
                <div hidden="{{!isCancelationResult}}">
                    <div hidden="{{!isOK}}">
                        <div class="layout horizontal" style="margin:0px 20px 0px 0px;">
                            <div style="margin:10px 0px 10px 0px;">
                                <button on-click="checkReceipt" style="margin: 0px 0px 0px 5px;">
                                    <i class="fa fa-certificate"></i>  <span>{{checkSignatureButtonMsg}}</span>
                                </button>
                            </div>
                            <div class="flex"></div>
                        </div>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'votevs-result-dialog',
            properties: {
                votevsReceipt:{type:String},
                hashCertVSHex:{type:String},
                hashCertVSBase64:{type:String},
                statusCode:{type:Number},
                messageType:{type:String, observer:'messageTypeChanged'},
                callerCallback:{type:String},
                voteVSCancellationReceipt:{type:String},
                checkSignatureButtonMsg:{type:String, value:'${msg.checkVoteLbl}'},
                appMessageJSON:{type:Object}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(appMessageJSON) {
                console.log(this.tagName + " - show - appMessageJSON: " + appMessageJSON)
                this.message = null
                this.caption = null
                this.optionSelected = null
                this.votevsReceipt = null
                this.appMessageJSON = appMessageJSON
                this.statusCode = appMessageJSON.statusCode
                this.isOK = (this.statusCode == 200)
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.caption = "${msg.voteOKCaption}"
                    this.message = "${msg.voteResultOKMsg}"
                    this.optionSelected = appMessageJSON.optionSelected
                    this.hashCertVSHex = appMessageJSON.hashCertVSHex
                    this.votevsReceipt = appMessageJSON.voteVSReceipt
                    this.hashCertVSBase64 = appMessageJSON.hashCertVSBase64
                    this.checkSignatureButtonMsg = '${msg.checkVoteLbl}'
                } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == appMessageJSON.statusCode) {
                    this.caption = '${msg.voteERRORCaption}'
                    var msgTemplate =  "${msg.accessRequestRepeatedMsg}"
                    this.message = msgTemplate.format(appMessageJSON.eventVS.subject, appMessageJSON.url);
                } else {
                    this.caption = '${msg.voteERRORCaption}'
                    this.message = appMessageJSON.message
                }
                this.messageType = "VOTE_RESULT"
                this.$.xDialog.opened = true
            },
            messageTypeChanged: function() {
                this.isVoteResult = false
                this.isCancelationResult = false
                this.isOK = (this.statusCode == 200)
                if("VOTE_RESULT" === this.messageType) this.isVoteResult = true
                if("VOTE_CANCELLATION_RESULT" === this.messageType) this.isCancelationResult = true
            },
            cancelVote: function() {
                this.checkSignatureButtonMsg = '${msg.checkReceiptLbl}'
                this.callerCallback = Math.random().toString(36).substring(7)
                showMessageVS('${msg.cancelVoteConfirmMsg}', '${msg.cancelVoteLbl}', this.callerCallback, true)
                this.$.xDialog.opened = false
            },
            confirmDialogClosed: function(e) {
                console.log("confirmDialogClosed - detail: " + e.detail)
                if(e.detail == this.callerCallback) this.show(this.appMessageJSON)
            },
            close: function() {
                this.$.xDialog.opened = false
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_SMIME)
                if(this.messageType == 'VOTE_RESULT') operationVS.message = this.votevsReceipt
                else if(this.messageType == 'VOTE_CANCELLATION_RESULT') operationVS.message = this.voteVSCancellationReceipt
                operationVS.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage)
                }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            cancellationConfirmed: function() {
                var operationVS = new OperationVS(Operation.CANCEL_VOTE)
                operationVS.message = this.hashCertVSBase64
                operationVS.serviceURL = contextURL + "/rest/voteVS/cancel"
                operationVS.signedMessageSubject = "${msg.cancelVoteLbl}"
                operationVS.setCallback(function(appMessage) { this.cancellationResponse(appMessage) }.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            cancellationResponse: function(appMessage) {
                console.log(this.tagName + " - cancellationResponse: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.messageType = "VOTE_CANCELLATION_RESULT"
                    this.voteVSCancellationReceipt = appMessageJSON.message;
                    this.message = "${msg.voteVSCancellationOKMsg}"
                    this.caption =  "${msg.voteVSCancellationCaption}"
                    this.checkSignatureButtonMsg = '${msg.checkReceiptLbl}'
                    this.$.xDialog.opened = true
                } else showMessageVS(appMessageJSON.message, '${msg.voteVSCancellationErrorCaption}')
                this.click()
            }
        });
    </script>
</dom-module>
