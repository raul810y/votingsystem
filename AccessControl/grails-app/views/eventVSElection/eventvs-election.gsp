<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-fab', file: 'paper-fab.html')}">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/eventVSElection/eventvs-election-voteconfirm-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/eventvs-admin-dialog']"/>">
<link rel="import" href="<g:createLink  controller="element" params="[element: '/element/votevs-result-dialog']"/>">

<polymer-element name="eventvs-election" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style></style>
        <div class="pageContentDiv">
            <template if="{{'admin' == menuType}}">
                <div class="text-center" style="">
                    <template if="{{'ACTIVE' == eventvs.state || 'PENDING' == eventvs.state}}">
                        <button type="submit" class="btn btn-warning" on-click="{{showAdminDialog}}"
                                style="margin:15px 20px 15px 0px;">
                            <g:message code="adminDocumentLinkLbl"/> <i class="fa fa fa-check"></i>
                        </button>
                    </template>
                </div>
            </template>

            <div style="margin: 0px 30px;">
            <div layout horizontal center center-justified style="width:100%;">
                <template if="{{subpage}}">
                    <paper-fab icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                </template>
                <div flex id="pageTitle" class="pageHeader text-center"><h3>{{eventvs.subject}}</h3></div>
            </div>

            <div layout horizontal style="width: 100%;">
                <div flex>
                    <template if="{{'PENDING' == eventvs.state}}">
                        <div style="font-size: 1.2em; font-weight:bold;color:#fba131;"><g:message code="eventVSPendingMsg"/></div>
                    </template>
                    <template if="{{'TERMINATED' == eventvs.state || 'CANCELLED' == eventvs.state}}">
                        <div style="font-size: 1.2em; font-weight:bold;color:#cc1606;"><g:message code="eventVSFinishedLbl"/></div>
                    </template>
                </div>
                <template if="{{'PENDING' == eventvs.state}}">
                    <div><b><g:message code="dateBeginLbl"/>: </b>
                        {{eventvs.dateBeginStr}}</div>
                </template>
                <div style="margin:0px 30px 0px 30px;"><b><g:message code="dateLimitLbl"/>: </b>
                    {{eventvs.dateFinishStr}}</div>
            </div>

            <div>
                <div class="eventContentDiv">
                    <votingsystem-html-echo html="{{eventvs.content}}"></votingsystem-html-echo>
                </div>

                <div id="eventAuthorDiv" class="text-right row" style="margin:0px 20px 20px 0px;">
                    <b><g:message code="publishedByLbl"/>: </b>{{eventvs.userVS}}
                </div>

                <div class="fieldsBox" style="">
                    <fieldset>
                        <legend><g:message code="pollFieldLegend"/></legend>
                        <div>
                            <template if="{{'ACTIVE' == eventvs.state}}">
                                <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                    <div class="btn btn-default btn-lg" on-click="{{showConfirmDialog}}"
                                         style="width: 90%;margin: 10px auto 30px auto; border: 2px solid #6c0404; padding: 10px; font-size: 1.2em;" >
                                        {{optionvs.content}}
                                    </div>
                                </template>
                            </template>
                            <template if="{{'ACTIVE' != eventvs.state}}">
                                <template repeat="{{optionvs in eventvs.fieldsEventVS}}">
                                    <div class="voteOption" style="width: 90%;margin: 10px auto 0px auto;">
                                        - {{optionvs.content}}
                                    </div>
                                </template>
                            </template>
                        </div>
                    </fieldset>
                </div>
            </div>
            </div>
        </div>
        <eventvs-vote-confirm-dialog id="confirmOptionDialog"></eventvs-vote-confirm-dialog>
        <eventvs-admin-dialog id="eventVSAdminDialog"></eventvs-admin-dialog>
        <votevs-result-dialog id="votevsResultDialog"></votevs-result-dialog>
    </template>
    <script>
        Polymer('eventvs-election', {
            menuType:menuType,
            publish: {
                eventvs: {value: {}}
            },
            subpage:false,
            optionVSSelected:null,
            eventvsChanged:function() {
                this.optionVSSelected = null
            },
            ready: function() {
                console.log(this.tagName + "- subpage:  " + this.subpage)
                this.$.confirmOptionDialog.addEventListener('optionconfirmed', function (e) {
                    this.submitVote()
                }.bind(this))
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.opened = true
            },
            back:function() {
                this.fire('core-signal', {name: "eventvs-election-closed", data: null});
            },
            showConfirmDialog: function(e) {
                console.log(this.tagName + " showConfirmDialog")
                this.optionVSSelected = e.target.templateInstance.model.optionvs
                this.$.confirmOptionDialog.show(this.optionVSSelected.content)
            },
            submitVote:function() {
                console.log("submitVote")
                var voteVS = {optionSelected:this.optionVSSelected, eventId:this.eventvs.id, eventURL:this.eventvs.URL}
                var webAppMessage = new WebAppMessage(ResponseVS.SC_PROCESSING, Operation.SEND_SMIME_VOTE)
                this.eventvs.voteVS = voteVS
                webAppMessage.eventVS = this.eventvs
                webAppMessage.signedMessageSubject = '<g:message code="sendVoteMsgSubject"/>'
                webAppMessage.setCallback(function(appMessage) {
                    console.log(this.tagName + " - vote callback - message: " + appMessage);
                    var appMessageJSON = toJSON(appMessage)
                    appMessageJSON.eventVS = this.eventvs
                    appMessageJSON.optionSelected = this.optionVSSelected.content
                    this.$.votevsResultDialog.show(appMessageJSON)
                }.bind(this))
                console.log(" - webAppMessage: " +  JSON.stringify(webAppMessage))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
            }
        });
    </script>
</polymer-element>