<%@ page import="org.votingsystem.model.TypeVS" %>
<asset:javascript src="utilsVS.js"/>
<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-button', file: 'paper-button.html')}">

<polymer-element name="message-smime-votevs-canceller" attributes="smimeMessageContent smimeMessage isClientToolConnected timeStampDate">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser {
                font-weight: bold;
                margin:10px auto 10px auto;
                background: #f9f9f9;
                padding:10px 20px 10px 20px;
            }
            .timeStampMsg {
                color:#aaaaaa; font-size:1.1em; margin:0 0 15px 0;font-style:italic;
            }
        </style>
        <div layout vertical style="margin: 10px auto; max-width:1000px;">
            <div  layout horizontal center center-justified>
                <div flex></div>
                <div class="pageHeader"><h3><g:message code="voteVSCancellerReceipt"/></h3></div>
                <div flex horizontal layout end-justified style="margin:10px 0px 10px 0px;">
                    <template if="{{isClientToolConnected}}">
                        <paper-button raised on-click="{{checkReceipt}}">
                            <i class="fa fa-certificate"></i>  <g:message code="checkSignatureLbl"/>
                        </paper-button>
                    </template>
                </div>
            </div>
            <div class="timeStampMsg" style="display:{{timeStampDate ? 'block':'none'}}">
                <b><g:message code="timeStampDateLbl"/>: </b>{{timeStampDate}}
            </div>
            <template if="{{messageToUser}}">
                <div  layout horizontal center center-justified  class="messageToUser">
                    <div>
                        <div id="messageToUser">{{messageToUser}}</div>
                    </div>
                    <paper-shadow z="1"></paper-shadow>
                </div>
            </template>
            <div><b><g:message code="hashAccessRequestLbl"/>: </b>{{smimeMessageContent.hashAccessRequestBase64}}</div>
            <div><b><g:message code="originHashAccessRequestLbl"/>: </b>{{smimeMessageContent.originHashAccessRequest}}</div>
            <div><b><g:message code="hashCertVSLbl"/>: </b>{{smimeMessageContent.hashCertVSBase64}}</div>
            <div><b><g:message code="originHashCertVote"/>: </b>{{smimeMessageContent.originHashCertVote}}</div>
        </div>
    </template>
    <script>
        Polymer('message-smime-votevs-canceller', {
            publish: {
                smimeMessageContent: {value: {}}
            },
            isClientToolConnected:window['isClientToolConnected'],
            messageToUser:null,
            timeStampDate:null,
            ready: function() {
                console.log(this.tagName + " - ready - " + document.querySelector("#voting_system_page"))
                document.querySelector("#voting_system_page").addEventListener('votingsystem-clienttoolconnected', function() {
                    this.isClientToolConnected = true
                }.bind(this))
            },
            smimeMessageContentChanged:function() {
                this.messageToUser = null
                if('${TypeVS.CANCEL_VOTE.toString()}' != this.smimeMessageContent.operation )
                    this.messageToUser = '<g:message code="smimeTypeErrorMsg"/>' + " - " + this.smimeMessageContent.operation
            },
            checkReceipt: function() {
                var webAppMessage = new WebAppMessage(Operation.OPEN_SMIME)
                webAppMessage.message = this.smimeMessage
                webAppMessage.setCallback(function(appMessage) {
                    console.log("saveReceiptCallback - message: " + appMessage)
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>