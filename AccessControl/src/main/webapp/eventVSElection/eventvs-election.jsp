<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/iron-media-query/iron-media-query.html" rel="import"/>
<link href="eventvs-election-vote-confirm-dialog.vsp" rel="import"/>
<link href="eventvs-admin-dialog.vsp" rel="import"/>
<link href="votevs-result-dialog.vsp" rel="import"/>
<link href="eventvs-election-stats.vsp" rel="import"/>


<dom-module name="eventvs-election">
    <template>
        <style>
            .tabContent { margin:0px auto 0px auto; width:auto; }
            .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px;}
            .representativeNumRepHeader { text-overflow: ellipsis; color:#888;}
            .statsTab {font-weight: bold; font-size: 1.1em; margin:0 40px 0 0; text-align: center; cursor:pointer; width: 100%;}
            .tabSelected { border-bottom: 2px solid #ba0011;}
        </style>
        <iron-media-query query="max-width: 600px" query-matches="{{smallScreen}}"></iron-media-query>
        <iron-ajax auto url="{{url}}" last-response="{{eventvs}}" handle-as="json"
                   content-type="application/json"></iron-ajax>
        <div>
            <div hidden="{{adminMenuHidden}}" style="text-align: center;">
                <button type="submit" on-click="showAdminDialog" style="margin:15px 20px 15px 0px;">
                    ${msg.adminDocumentLinkLbl} <i class="fa fa fa-check"></i>
                </button>
            </div>

            <div style="margin: 0px 30px;">
                <div class="layout horizontal center center-justified" style="width:100%;">
                    <div>
                        <paper-fab hidden="{{!votevsResul}}" mini icon="mail" on-click="showVoteResul"
                                   style="color: #ba0011;margin: 0 0 0 20px;background: #ffeb3b;"></paper-fab>
                    </div>

                    <div class="flex" style="text-align: center">
                        <div id="pageTitle" data-eventvs-id$="{{eventvs.id}}" class="pageHeader">{{eventvs.subject}}</div>
                    </div>

                    <div hidden="{{!isActive}}" style='color: #888;'>{{getElapsedTime(eventvs.dateFinish)}}</div>
                </div>
                <div class$="{{eventStateRowClass}}">
                    <div class="flex" style="display: block;">
                        <div hidden="{{!isPending}}" style="font-size: 1.3em; font-weight:bold;color:#fba131;">${msg.eventVSPendingMsg}</div>
                        <div hidden="{{!isTerminated}}" style="font-size: 1.3em; font-weight:bold;color:#cc1606;">${msg.eventVSFinishedLbl}</div>
                        <div hidden="{{!isCanceled}}" style="font-size: 1.3em; font-weight:bold;color:#cc1606;">${msg.eventVSCancelledLbl}</div>
                    </div>
                    <div style="margin:0px 30px 0px 30px; color: #888;"><b>${msg.dateLbl}: </b>
                        <span>{{getDate(eventvs.dateBegin)}}</span></div>
                </div>
                <div>
                    <div class="eventContentDiv">
                        <vs-html-echo html="{{decodeBase64(eventvs.content)}}"></vs-html-echo>
                    </div>

                    <div class="horizontal layout center center-justified">
                        <div hidden="{{!isTerminated}}" style="margin: 10px 0 0 0;">
                            <button on-click="getResults">
                                <i class="fa fa-bar-chart"></i> ${msg.getResultsLbl}
                            </button>
                        </div>
                        <div id="eventAuthorDiv" class="flex" style="margin:0px 20px 0 30px; color:#888; font-size: 0.85em;">
                            <b>${msg.publishedByLbl}:</b> <span>{{eventvs.userVS}}</span>
                        </div>
                    </div>

                    <div class="horizontal layout" hidden="{{!smallScreen}}" style="margin: 20px 0 0 0;">
                        <div id="pollDiv" on-click="setPollView" class="statsTab">${msg.pollFieldLegend}</div>
                        <div id="resultsDiv"  on-click="setResultsView"  class="statsTab">${msg.resultsLbl}</div>
                    </div>
                    <div class="horizontal layout">
                        <div hidden="{{optionsDivHidden}}" style="width: 100%; display: block;">
                            <div>
                                <div hidden="{{!isActive}}">
                                    <div style="font-size: 1.4em; font-weight: bold; text-decoration: underline; color:#888;
                                        margin: 20px 0 0 0;">${msg.pollFieldLegend}:</div>
                                    <template is="dom-repeat" items="{{eventvs.fieldsEventVS}}">
                                        <div>
                                            <button on-click="showConfirmDialog"
                                                    style="margin: 30px 0px 0px 5px;font-size: 1.2em; font-weight:bold;min-width: 500px; padding: 10px;">
                                                <span>{{item.content}}</span>
                                            </button>
                                        </div>
                                    </template>
                                </div>
                                <div hidden="{{isActive}}">
                                    <div style="font-size: 1.4em; font-weight: bold; text-decoration: underline; color:#888;
                                        margin: 20px 0 10px 0;">${msg.pollFieldLegend}:</div>
                                    <template is="dom-repeat" items="{{eventvs.fieldsEventVS}}">
                                        <div class="voteOption" style="width: 90%;margin: 0px auto 15px auto;
                                            font-size: 1.3em; font-weight: bold;">
                                            - <span>{{item.content}}</span>
                                        </div>
                                    </template>
                                </div>
                            </div>
                        </div>
                        <div id="statsDiv" hidden="{{statsDivHidden}}" class="vertical layout center center-justified">
                            <eventvs-election-stats id="electionStats" eventvs-id="{{eventvs.id}}"></eventvs-election-stats>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <eventvs-election-vote-confirm-dialog id="confirmOptionDialog" on-option-confirmed="submitVote">
            </eventvs-election-vote-confirm-dialog>
        <eventvs-admin-dialog id="eventVSAdminDialog" eventvs="{{eventvs}}"></eventvs-admin-dialog>
        <votevs-result-dialog id="votevsResultDialog"></votevs-result-dialog>
    </template>
    <script>
        Polymer({
            is:'eventvs-election',
            properties: {
                eventvs:{type:Object, value:{}, observer:'eventvsChanged'},
                smallScreen:{type:Boolean, value:false, observer:'smallScreenChanged'},
                selectedTab:{type:String, value:'optionsTab', observer:'smallScreenChanged'}
            },
            ready: function() {
                console.log(this.tagName + "- ready")
                this.statsDivHidden = false
                sendSignalVS({caption:"${msg.pollLbl}"})
            },
            fireSignal:function() {
                this.fire('iron-signal', {name: "vs-innerpage", data: {caption:"${msg.pollLbl}"}});
            },
            decodeBase64:function(base64EncodedString) {
                if(base64EncodedString == null) return null
                return decodeURIComponent(escape(window.atob(base64EncodedString)))
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            getElapsedTime: function(dateStamp) {
                return new Date(dateStamp).getElapsedTime() + " ${msg.toCloseLbl}"
            },
            setResultsView: function() {
                this.selectedTab = 'statsTab'
                this.$.resultsDiv.className = 'tab tabSelected'
                this.$.pollDiv.className = 'tab'
                this.setTabs()
            },
            setPollView: function() {
                this.selectedTab = 'optionsTab'
                this.$.pollDiv.className = 'tab tabSelected'
                this.$.resultsDiv.className = 'tab'
            },
            setTabs:function() {
                if(this.selectedTab == 'optionsTab') {
                    this.statsDivHidden = true
                    this.setPollView()
                } else if(this.selectedTab == 'statsTab') {
                    this.optionsDivHidden = true
                    this.setResultsView()
                }
            },
            smallScreenChanged:function() {
                console.log("smallScreenChanged - smallScreen: " + this.smallScreen)
                this.statsDivHidden = false
                this.optionsDivHidden = false
                if(this.smallScreen) {
                    this.eventStateRowClass = "vertical layout flex"
                    this.setTabs()
                } else this.eventStateRowClass = "horizontal layout flex"
            },
            eventvsChanged:function() {
                this.$.electionStats
                console.log("eventvsChanged - eventvs: " + this.eventvs.state)
                this.optionVSSelected = null
                this.dateFinish = new Date(this.eventvs.dateFinish)
                this.votevsResul = null;
                this.isActive = false
                this.isPending = false
                this.isTerminated = false
                this.isCanceled = false
                if('PENDING' == this.eventvs.state) {
                    this.isPending = true
                    this.$.statsDiv.style.display = 'none'
                } else if ('TERMINATED' == this.eventvs.state) {
                    this.isTerminated = true
                } else if ('CANCELED' == this.eventvs.state) {
                    this.isCanceled = true
                } else this.isActive = true
                this.adminMenuHidden = true
                if('admin' === menuType) {
                    if(this.eventvs.state === 'ACTIVE' || this.eventvs.state === 'PENDING') this.adminMenuHidden = false
                }
                this.$.electionStats.eventvsId = this.eventvs.id
            },
            showAdminDialog:function() {
                this.$.eventVSAdminDialog.show()
            },
            back:function() {
                this.fire('eventvs-election-closed');
            },
            showConfirmDialog: function(e) {
                console.log(this.tagName + " showConfirmDialog")
                if(!window['isClientToolConnected']) {
                    showMessageVS("${msg.clientToolRequiredErrorMsg}", "${msg.errorLbl}");
                } else {
                    this.optionVSSelected = e.model.item
                    this.$.confirmOptionDialog.show(this.optionVSSelected.content)
                }
            },
            getResults:function() {
                console.log("getResults")
                var fileURL = "${contextURL}/static/backup/" + this.dateFinish.urlFormat() + "/VOTING_EVENT_" + this.eventvs.id + ".zip"
                if(window['isClientToolConnected']) {
                    var operationVS = new OperationVS(Operation.FILE_FROM_URL)
                    operationVS.subject = '${msg.downloadingFileMsg}'
                    operationVS.documentURL = fileURL
                    operationVS.setCallback(function(appMessage) { this.showGetResultsResponse(appMessage) }.bind(this))
                    VotingSystemClient.setMessage(operationVS)
                } else window.location.href  = fileURL
            },
            showGetResultsResponse:function(appMessage) {
                var appMessageJSON = toJSON(appMessage)
                if(ResponseVS.SC_OK !== appMessageJSON.statusCode) showMessageVS(appMessageJSON.message, "${msg.errorLbl}")
                this.click()//hack to refresh screen
            },
            showVoteResul:function() {
                this.$.votevsResultDialog.show(this.votevsResul)
            },
            submitVote:function() {
                console.log("submitVote - eventvs.url: " + this.eventvs.url)
                var voteVS = {optionSelected:this.optionVSSelected, eventVSId:this.eventvs.id, eventURL:this.eventvs.url}
                var operationVS = new OperationVS(Operation.SEND_VOTE)
                this.eventvs.voteVS = voteVS
                operationVS.voteVS = voteVS
                operationVS.signedMessageSubject = '${msg.sendVoteMsgSubject}'
                operationVS.setCallback(function(appMessage) { this.submitVoteResponse(appMessage)}.bind(this))
                console.log(" - operationVS: " +  JSON.stringify(operationVS))
                VotingSystemClient.setMessage(operationVS);
            },
            submitVoteResponse:function(appMessage) {
                console.log(this.tagName + " - vote callback - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                appMessageJSON.eventVS = this.eventvs
                appMessageJSON.optionSelected = this.optionVSSelected.content
                this.votevsResul = appMessageJSON
                this.$.votevsResultDialog.show(appMessageJSON)
                this.click(); //hack to refresh screen
            }
            });
    </script>
</dom-module>