<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-icon" file="core-icon.html"/>
<vs:webresource dir="vs-user-box" file="vs-user-box.html"/>
<vs:webresource dir="vs-currency-selector" file="vs-currency-selector.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webresource dir="paper-button" file="paper-button.html"/>
<vs:webresource dir="paper-shadow" file="paper-shadow.html"/>
<vs:webresource dir="paper-radio-button" file="paper-radio-button.html"/>
<vs:webcomponent path="/tagVS/tagvs-select-dialog"/>
<vs:webcomponent path="/userVS/uservs-selector-dialog"/>


<polymer-element name="transactionvs-form" attributes="subpage">
<template>
        <g:include view="/include/styles.gsp"/>
        <style no-shim>
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .container{ margin: 0 auto; max-width: 550px; }
        .adviceToUser {color: #888;font-size: 1.2em; margin: 10px 0 0 0; font-weight: bold;}
        </style>
        <div class="container">

            <div layout horizontal center center-justified style="">
                <template if="{{subpage}}">
                    <div style="margin: 10px 0 0 0;" title="<g:message code="backLbl"/>" >
                        <paper-fab mini icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                    </div>
                </template>
                <div flex  style="text-align: center; color:#6c0404;">
                    <div>
                        <div style="font-size: 1.5em; font-weight: bold;">{{operationMsg}}</div>
                        <div>{{fromUserName}}</div>
                    </div>
                </div>
            </div>

            <div style="display:{{messageToUser?'block':'none'}};">
                <div style="color: {{status == 200?'#388746':'#ba0011'}};">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                            <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{status == 200?'#388746':'#ba0011'}};"></core-icon>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>
            </div>
            <template if="{{'FROM_USERVS_TO_USERVS' === operation}}">
                <div class="adviceToUser"><g:message code="fromUserVSToUserVSAdviceMsg"/></div>
            </template>


            <div layout vertical id="formDataDiv" style="padding: 0px 20px 0px 20px; height: 100%;">
                <div>
                    <div horizontal layout center center-justified>
                        <input type="text" id="amount" class="form-control" style="width:150px;margin:0 10px 0 0;" pattern="^[0-9]*$" required
                               title="<g:message code="amountLbl"/>" placeholder="<g:message code="amountLbl"/>"/>
                        <vs-currency-selector id="currencySelector"></vs-currency-selector>
                    </div>
                    <div>
                        <input type="text" id="transactionvsSubject" class="form-control" required
                               title="<g:message code="subjectLbl"/>" placeholder="<g:message code="subjectLbl"/>"/>
                    </div>
                </div>

                <div layout horizontal style="margin:15px 0px 15px 0px; border: 1px solid #ccc;
                    font-size: 1.1em; padding: 5px;display: block;}}">
                    <div style="margin:0px 10px 0px 0px; padding:5px;">
                        <div style="display: {{selectedTags.length > 0 ? 'block':'none'}}" >
                            <div layout horizontal center center-justified style="margin: 0 0 20px 0;">
                                <div style="margin: 0 10px 0 0;"><paper-radio-button id="timeLimitedRButton" toggles/></div>
                                <div style="color:#6c0404; font-size: 1.1em;"><g:message code="timeLimitedAdviceMsg"/></div>
                            </div>
                        </div>
                        <template if="{{selectedTags.length === 0}}">
                            <div layout horizontal center center-justified style="font-size: 0.8em;">
                                <div style="width: 180px; margin:0 10px 0 0;">
                                    <paper-button raised on-click="{{showTagDialog}}" style="font-size: 1em;
                                    margin:10px 0px 10px 10px;display:{{(isPending || isCancelled ) ? 'none':'block'}} ">
                                        <i class="fa fa-tag"></i> <g:message code="addTagLbl"/>
                                    </paper-button>
                                </div>
                                <div style="display: {{selectedTags.length == 0? 'block':'none'}};">
                                    <g:message code="transactionvsWithTagAdvertMsg"/>
                                </div>
                                <div style="max-width: 400px;">{{selectedTagMsg}}</div>
                            </div>
                        </template>
                        <div layout horizontal center center-justified style="font-weight:bold;text-align: center;
                            display: {{selectedTags.length == 0? 'none':'block'}};">
                            <div><g:message code="selectedTagsAdvice"/></div>
                            <template repeat="{{tag in selectedTags}}">
                                <div layout horizontal center center-justified style="font-size: 0.9em;">
                                    <paper-button raised on-click="{{removeTag}}" style="margin:0px; padding:0px; color:#6c0404;">
                                        <i class="fa fa-minus"></i>
                                    </paper-button>
                                    <a class="btn btn-default" data-tagId='{{tag.id}}' style="font-size: 0.9em;
                                        margin:5px 5px 0px 5px;padding:3px;"><i class="fa fa-tag"></i> {{tag.name}}
                                    </a>
                                </div>
                            </template>
                        </div>
                    </div>
                </div>
                <div style="display:{{isWithUserSelector?'block':'none'}}">
                    <div>
                        <paper-button raised on-click="{{openSearchUserDialog}}" style="margin: 0 0 5px 5px;">
                            <i class="fa fa-user"></i> {{selectReceptorMsg}}
                        </paper-button>
                        <div style="margin:10px 0 0 0;">
                            <vs-user-box flex id="receptorBox" boxCaption="<g:message code="receptorLbl"/>"></vs-user-box>
                        </div>
                    </div>
                </div>
                <template if="{{!isWithUserSelector}}">
                    <div style="margin:10px 0 0 0; text-align: center; font-weight: bold; color:#6c0404;">{{selectReceptorMsg}}</div>
                </template>
                <div flex>
                </div>
                <div layout horizontal style="margin:10px 20px 0px 0px;">
                    <div flex></div>
                    <paper-button raised on-click="{{submitForm}}" style="margin: 20px 0px 0px 5px;">
                        <i class="fa fa-check"></i> <g:message code="acceptLbl"/>
                    </paper-button>
                </div>
            </div>
        </div>

        <uservs-selector-dialog id="userVSSelectorDialog" groupVSId="{{groupId}}"></uservs-selector-dialog>
    <div>
        <div layout horizontal center center-justified style="padding:100px 0px 0px 0px;margin:0px auto 0px auto;">
            <tagvs-select-dialog id="tagDialog" caption="<g:message code="addTagDialogCaption"/>"
                serviceURL="<g:createLink controller="tagVS" action="index" />"></tagvs-select-dialog>
        </div>
    </div>
</template>
<script>
    Polymer('transactionvs-form', {
        operation:null,
        maxNumberTags:1,
        fromUserName:null,
        fromUserIBAN:null,
        toUserName:null,
        groupId:null,
        selectedTags: [],
        subpage:false,
        ready: function() {
            console.log(this.tagName + " - " + this.id)
            this.isWithUserSelector = false

            if(document.querySelector("#coreSignals")) {
                document.querySelector("#coreSignals").addEventListener('core-signal-user-clicked', function(e) {
                    if(this.$.receptorBox) this.$.receptorBox.addUser(e.detail)
                }.bind(this));
            }

            this.$.tagDialog.addEventListener('tag-selected', function (e) {
                console.log("tag-selected: " + JSON.stringify(e.detail))
                this.selectedTags = e.detail
            }.bind(this))

        },
        openSearchUserDialog:function(){
            this.$.userVSSelectorDialog.show()
        },
        showTagDialog: function() {
            this.$.tagDialog.show(this.maxNumberTags, this.selectedTags)
        },
        selectedTagsChanged: function(e) {
            if(this.selectedTags.length > 0) this.selectedTagMsg = "<g:message code="onlyTagAllowedExpendingMsg"/>"
            else this.selectedTagMsg = null
        },
        removeTag: function(e) {
            var tagToDelete = e.target.templateInstance.model.tag
            for(tagIdx in this.selectedTags) {
                if(tagToDelete.id == this.selectedTags[tagIdx].id) {
                    this.selectedTags.splice(tagIdx, 1)
                }
            }
        },

        reset: function() {
            console.log(this.id + " - reset")
            this.removeErrorStyle(this.$.formDataDiv)
            this.isWithUserSelector = false
            this.$.amount.value = ""
            this.$.transactionvsSubject.value = ""
            this.setMessage(200, null)
            this.$.receptorBox.removeUsers()
            this.$.tagDialog.reset()
        },

        removeErrorStyle: function (element) {
            var formElements = element.children
            for(var i = 0; i < element.childNodes.length; i++) {
                var child = element.childNodes[i];
                this.removeErrorStyle(child);
                if(child != undefined) {
                    if(child.style != undefined) {
                        child.style.background = '#fff'
                        child.classList.remove("formFieldError");
                    }
                }
            }
        },

        submitForm: function () {
            this.removeErrorStyle(this.$.formDataDiv)
            switch(this.operation) {
                case Operation.FROM_GROUP_TO_MEMBER:
                    if(this.$.receptorBox.getUserList().length == 0){
                        this.setMessage(500, "<g:message code='receptorMissingErrorLbl'/>")
                        return false
                    }
                    break;
                case Operation.FROM_GROUP_TO_MEMBER_GROUP:
                    if(this.$.receptorBox.getUserList().length == 0){
                        this.setMessage(500, "<g:message code='receptorMissingErrorLbl'/>")
                        return false
                    }
                    break;
                case Operation.FROM_GROUP_TO_ALL_MEMBERS:
                    break;
            }
            if(!this.$.amount.validity.valid) {
                //this.$.amount.classList.add("formFieldError")
                this.$.amount.style.background = '#f6ccd0'
                this.setMessage(500, "<g:message code='enterValidAmountMsg'/>")
                return
            }

            if(!this.$.transactionvsSubject.validity.valid) {
                //this.$.transactionvsSubject.classList.add("formFieldError")
                this.$.transactionvsSubject.style.background = '#f6ccd0'
                this.setMessage(500, "<g:message code='emptyFieldMsg'/>")
                return
            }
            this.setMessage(200, null)

            var tagList = []
            if(this.selectedTags.length > 0) {
                for(tagIdx in this.selectedTags) {
                    //tagList.push({id:this.selectedTags[tagIdx].id, name:this.selectedTags[tagIdx].name});
                    tagList.push(this.selectedTags[tagIdx].name);
                }
            } else tagList.push('WILDTAG'); //No tags, receptor can expend money with any tag

            var webAppMessage = new WebAppMessage( this.operation)
            webAppMessage.serviceURL = "${createLink( controller:'transactionVS', action:" ", absolute:true)}"
            webAppMessage.signedMessageSubject = "<g:message code='transactionvsFromGroupMsgSubject'/>"
            webAppMessage.signedContent = {operation:this.operation, subject:this.$.transactionvsSubject.value,
                isTimeLimited:this.$.timeLimitedRButton.checked, tags:tagList, amount: this.$.amount.value,
                currencyCode:this.$.currencySelector.getSelected(), fromUser:this.fromUserName,
                fromUserIBAN:this.fromUserIBAN}
            if(this.toUserName)  webAppMessage.signedContent.toUser = this.toUserName
            if(this.getToUserIBAN()) webAppMessage.signedContent.toUserIBAN = this.getToUserIBAN()
            webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    var caption
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        caption = "<g:message code='transactionvsOKLbl'/>"
                        this.fire('operation-finished')
                    } else caption = '<g:message code="transactionvsERRORLbl"/>'
                    showMessageVS(appMessageJSON.message, caption)
                }.bind(this))
            VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
        },
        getToUserIBAN: function () {
            if(this.operation === Operation.FROM_GROUP_TO_ALL_MEMBERS) return null
            else {
                var receptorList = this.$.receptorBox.getUserList()
                var result = []
                for(userIdx in receptorList) {
                    result.push(receptorList[userIdx].IBAN);
                }
                return result
            }
        },
        setMessage:function(status, message) {
            console.log(this.tagName + " - setMessage - status: " + status, " - message: " + message)
            this.status = status
            this.messageToUser = message
        },
        back:function() {
            this.fire('operation-finished')
        },
        init:function(operation, userName, userIBAN, targetGroupId) {
            console.log(this.id + " - init - operation: " + operation + " - subpage: " + this.subpage +
                    " - userIBAN: " + userIBAN)
            this.reset()
            this.operation = operation
            this.fromUserName = userName
            this.fromUserIBAN = userIBAN
            this.toUserIBAN = null
            this.groupId = targetGroupId
            this.$.transactionvsSubject.value = ""
            this.$.amount.value = ""
            this.isWithUserSelector = true
            this.toUserName = null
            switch(operation) {
                case Operation.FROM_GROUP_TO_MEMBER:
                    this.operationMsg = "<g:message code='transactionVSFromGroupToMember'/>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorMsg"/>'
                    this.$.receptorBox.multiSelection = false
                    break;
                case Operation.FROM_GROUP_TO_MEMBER_GROUP:
                    this.operationMsg = "<g:message code='transactionVSFromGroupToMemberGroup'/>"
                    this.selectReceptorMsg = '<g:message code="selectReceptorsMsg"/>'
                    this.$.receptorBox.multiSelection = true
                    break;
                case Operation.FROM_GROUP_TO_ALL_MEMBERS:
                    this.isWithUserSelector = false
                    this.operationMsg = "<g:message code='transactionVSFromGroupToAllMembers'/>"
                    this.selectReceptorMsg = '<g:message code="transactionvsToAllGroupMembersMsg"/>'
                    break;
                case Operation.FROM_USERVS:
                case Operation.FROM_USERVS_TO_USERVS:
                    this.operationMsg = "<g:message code='transactionVSFromUserVS'/> <g:message code='forLbl'/> '" +
                            userName + "'"
                    this.fromUserName = null
                    this.fromUserIBAN = null
                    this.isWithUserSelector = false
                    this.toUserName = userName
                    this.$.receptorBox.uservsList= [{name:userName, IBAN:userIBAN}]
                    break;
            }
            this.selectedTags = []
            return this.caption
        }
    });
</script>
</polymer-element>
