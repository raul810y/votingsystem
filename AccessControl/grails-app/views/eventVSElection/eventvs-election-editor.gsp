<vs:webresource dir="vs-texteditor" file="vs-texteditor.html"/>
<vs:webcomponent path="/element/eventvs-option-dialog"/>
<vs:webresource dir="paper-shadow" file="paper-shadow.html"/>

<polymer-element name="eventvs-election-editor">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .messageToUser { font-weight: bold; margin:10px auto 10px auto;  padding:10px 20px 10px 20px; }
        </style>
        <core-signals on-core-signal-messagedialog-closed="{{messageDialogClosed}}"></core-signals>
        <div class="pageHeader"  layout horizontal center center-justified style="margin: 10px 0 10px 0;">
            <g:message code="publishVoteLbl"/>
        </div>

        <div style="display:{{messageToUser? 'block':'none'}}">
            <div class="messageToUser">
                <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                    <div id="messageToUser">{{messageToUser}}</div>
                </div>
                <paper-shadow z="1"></paper-shadow>
            </div>
        </div>

        <form id="mainForm" on-submit="{{submitForm}}">

            <div style="margin:0px 0px 20px 0px">
                <div>
                    <input type="text" id="subject" class="form-control" style="width:600px;"
                           placeholder="<g:message code="subjectLbl"/>" error="<g:message code="requiredLbl"/>" required/>
                </div>
                <div layout horizontal center id="dateRangeDiv" style="margin:10px 0px 0px 0px;">
                    <label><g:message code="electionDateLbl"/></label>
                    <div id="dateBegin">
                        <g:datePicker name="dateBegin" value="${new Date().plus(1)}" precision="day" relativeYears="[0..1]"/>
                    </div>
                </div>
            </div>

            <div style="position:relative; width:100%;">
                <vs-texteditor id="textEditor" type="pc" style="height:300px; width:100%;"></vs-texteditor>
            </div>

            <div id="fieldsDiv" style="display:{{pollOptionList.length == 0? 'none':'block'}}">
                <div class="fieldsBox">
                    <fieldset>
                        <legend><g:message code="pollFieldLegend"/></legend>
                        <div layout vertical>
                            <template repeat="{{pollOption in pollOptionList}}">
                                <div>
                                    <a class="btn btn-default" on-click="{{removePollOption}}" style="font-size: 0.9em; margin:5px 5px 0px 0px;padding:3px;">
                                        <g:message code="deleteLbl"/> <i class="fa fa-minus"></i></a>
                                    {{pollOption}}
                                </div>

                            </template>
                        </div>
                    </fieldset>
                </div>
            </div>

            <div layout horizontal center center-justified style="margin: 15px auto 30px auto;padding:0px 10px 0px 10px;">
                <div>
                    <paper-button raised id="addOptionButton" on-click={{showVotingOptionDialog}}">
                        <i class="fa fa-plus"></i> <g:message code="addOptionLbl"/>
                    </paper-button>
                </div>
                <div flex></div>
                <paper-button raised on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                    <i class="fa fa-check"></i> <g:message code="publishLbl"/>
                </paper-button>
            </div>
        </form>

    </div>

        <eventvs-option-dialog id="addVotingOptionDialog"></eventvs-option-dialog>
    </template>
    <script>
        Polymer('eventvs-election-editor', {
            appMessageJSON:null,
            pollOptionList : [],
            ready: function() {
                console.log(this.tagName + " - ready")
                //alert( CKEDITOR.basePath );
                this.$.addVotingOptionDialog.addEventListener('on-submit', function (e) {
                    this.pollOptionList.push(e.detail)
                }.bind(this))
            },
            showVotingOptionDialog: function() {
                this.$.addVotingOptionDialog.show()
            },
            removePollOption: function(e) {
                var pollOption = e.target.templateInstance.model.pollOption
                console.log("removePollOption")
                for(optionIdx in this.pollOptionList) {
                    console.log("option: " +  this.pollOptionList[optionIdx] + " - pollOption: " + pollOption)
                    if(pollOption == this.pollOptionList[optionIdx]) {
                        this.pollOptionList.splice(optionIdx, 1)
                    }
                }
            },
            submitForm: function() {
                this.messageToUser = null
                this.$.subject.classList.remove("formFieldError");
                var dateBegin = getDatePickerValue('dateBegin', this.$.dateBegin)
                if(!this.$.subject.validity.valid) {
                    this.$.subject.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(dateBegin == null) {
                    this.messageToUser = '<g:message code="emptyFieldMsg"/>'
                    return
                }

                if(this.$.textEditor.getData().length == 0) {
                    this.$.textEditor.classList.add("formFieldError");
                    this.messageToUser = '<g:message code="emptyDocumentERRORMsg"/>'
                    return
                }


                if(this.pollOptionList.length < 2) { //two options at least
                    this.messageToUser = '<g:message code="optionsMissingERRORMsg"/>'
                    this.$.addOptionButton.classList.add( "formFieldError" );
                    return
                }

                var pollOptions = []
                for(optionIdx in this.pollOptionList) {
                    var option = {content:this.pollOptionList[optionIdx]}
                    pollOptions.push(option)
                }

                var eventVS = {};
                eventVS.subject = this.$.subject.value;
                eventVS.content = this.$.textEditor.getData();
                eventVS.dateBegin = dateBegin.formatWithTime();
                eventVS.fieldsEventVS = pollOptions
                this.appMessageJSON = null

                var webAppMessage = new WebAppMessage(Operation.VOTING_PUBLISHING)
                webAppMessage.signedContent = eventVS
                webAppMessage.serviceURL = "${createLink(controller:'eventVSElection', absolute:true)}"
                webAppMessage.signedMessageSubject = "<g:message code="publishVoteSubject"/>"
                webAppMessage.setCallback(function(appMessage) {
                    console.log("publishDocumentCallback - message: " + appMessage);
                    this.appMessageJSON = toJSON(appMessage)
                    electionDocumentURL = null
                    if(this.appMessageJSON != null) {
                        var caption = '<g:message code="publishERRORCaption"/>'
                        var msg = this.appMessageJSON.message
                        if(ResponseVS.SC_OK == this.appMessageJSON.statusCode) {
                            caption = '<g:message code="publishOKCaption"/>'
                            var msgTemplate = "<g:message code='documentLinkMsg'/>";
                            msg = "<p><g:message code='publishOKMsg'/>.</p>" +  msgTemplate.format(this.appMessageJSON.message);
                        }
                        showMessageVS(msg, caption)
                    }
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage)
            },
            messageDialogClosed:function() {
                if(this.appMessageJSON != null && this.appMessageJSON.message != null &&
                        ResponseVS.SC_OK == this.appMessageJSON.statusCode)
                    window.location.href = this.appMessageJSON.message
            }
        });
    </script>

</polymer-element>