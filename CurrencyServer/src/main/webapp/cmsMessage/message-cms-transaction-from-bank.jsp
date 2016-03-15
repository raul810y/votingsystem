<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="message-cms-transaction-from-bank">
    <template>
        <style>
        .actorLbl {font-size: 1.2em; text-decoration: underline;font-weight: bold; color: #621; }
        .timeStampMsg { color:#aaaaaa; font-size:1em; margin:0 0 15px 0;font-style:italic; }
        .iban-link {text-decoration: underline; color: #0000ee; cursor: pointer;}
        </style>
        <div class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
            <div class="" style="margin: 0px auto; color: #667;">
                <div class="pageHeader" style="margin:0 0 0 20px;font-size: 1.5em;text-align: center;">
                    ${msg.transactionFromBank}
                </div>
                <div hidden="{{!timeStampDate}}" class="timeStampMsg">
                    <b>${msg.dateLbl}: </b> <span>{{timeStampDate}}</span>
                </div>
                <div id="transactionTypeMsg" style="font-size: 1.5em; font-weight: bold;"></div>
                <div><b>${msg.subjectLbl}: </b>{{cmsMessageContent.subject}}</div>
                <div class="horizontal layout">
                    <div class="flex"><b>${msg.amountLbl}: </b>{{cmsMessageContent.amount}} {{cmsMessageContent.currencyCode}}</div>
                    <div hidden="{{!cmsMessageContent.timeLimited}}" class="pageHeader"
                         style="margin: 0 20px 0 0;"><b>${msg.timeLimitedLbl}</b> ${msg.timeLimitedDateMsg} <span>{{cmsMessageContent.validTo}}</span>
                    </div>
                </div>
                <div style="margin-left: 20px;">
                    <div class="actorLbl" style=" margin:10px 0px 0px 0px;">${msg.senderLbl}</div>
                    <div>
                        <div><b>${msg.bankLbl}: </b><span>{{cmsMessageContent.fromUserName.name}}</span> -
                            <span class="iban-link" on-click="showFromUserByIBAN">{{cmsMessageContent.fromUserName.iban}}</span></div>
                        <div><b>${msg.nameLbl}: </b><span>{{cmsMessageContent.fromUserName}}</span></div>
                        <div><b>${msg.IBANLbl}: </b><span>{{cmsMessageContent.fromUserIBAN}}</span></div>
                    </div>
                </div>
                <div hidden="{{!isReceptorVisible}}" style="margin:20px 0px 0px 20px;">
                    <div class="actorLbl">${msg.receptorLbl}</div>
                    <div class="layout horizontal">
                        <div><b>${msg.IBANLbl}: </b></div>
                        <div>
                            <template is="dom-repeat" items="{{cmsMessageContent.toUserIBAN}}" as="IBAN">
                                <div on-click="showToUserByIBAN" class="iban-link">{{IBAN}}</div>
                            </template>
                        </div>
                    </div>
                </div >
                <div class="horizontal layout" style="margin: 15px 0 0 0; width: 100%;">
                    <div hidden="{{tagsHidden}}" layout horizontal center center-justified>
                        <template is="dom-repeat" items="{{signedDocument.tags}}" as="tag">
                            <a class="btn btn-default" style="font-size: 0.7em;">
                                <i class="fa fa-tag" style="color: #888;"></i> {{tag}}</a>
                        </template>
                    </div>
                    <div class="flex"></div>
                    <div hidden="{{!isClientToolConnected}}" class="flex horizontal layout end-justified" style="margin:10px 0px 10px 0px;">
                        <button on-click="checkReceipt" style="color: #388746;">
                            <i class="fa fa-x509Certificate"></i>  ${msg.checkSignatureLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>

    </template>
    <script>
        Polymer({
            is:'message-cms-transaction-from-bank',
            properties: {
                cmsMessageContent: {type:Object},
                isClientToolConnected: {type:Boolean, value: false},
                tagsHidden: {type:Boolean, value: true},
                isReceptorVisible: {type:Boolean, value: true},
                cmsMessage: {type:String},
                timeStampDate: {type:String},
                caption: {type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
                sendSignalVS({caption:"${msg.transactionFromBank}"})
            },
            showFromUserByIBAN:function(e) {
                window.open(vs.contextURL + "/#!" + vs.contextURL + "/rest/user/IBAN/" + this.cmsMessageContent.fromUserIBAN, "_blank")
            },
            showToUserByIBAN:function(e) {
                console.log(this.tagName + " - showUserByIBAN:" + e)
                window.open(vs.contextURL + "/#!" + vs.contextURL + "/rest/user/IBAN/" + e.model.IBAN, "_blank")
            },
            checkReceipt: function() {
                var operationVS = new OperationVS(Operation.OPEN_CMS)
                operationVS.message = this.cmsMessage
                console.log(JSON.stringify(operationVS))
                VotingSystemClient.setMessage(operationVS);
            }
        });
    </script>
</dom-module>