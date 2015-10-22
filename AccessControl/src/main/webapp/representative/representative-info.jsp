<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../element/image-viewer-dialog.vsp" rel="import"/>
<link href="representative-select-dialog.vsp" rel="import"/>
<link href="representative-request-accreditations-dialog.vsp" rel="import"/>
<link href="representative-request-votinghistory-dialog.vsp" rel="import"/>

<dom-module name="representative-info">
    <template>
        <style>
            .tabContent { margin:0px auto 0px auto; width:auto; }
            .representativeNameHeader { font-size: 1.3em; text-overflow: ellipsis; color:#6c0404; padding: 0 40px 0 40px; text-align: center;}
            .representativeNumRepHeader { text-overflow: ellipsis; color:#888;}
            .tab {font-weight: bold; font-size: 1.1em; margin:0 40px 0 0; text-align: center; cursor:pointer; width: 100%;}
            .tabSelected { border-bottom: 2px solid #ba0011;}
        </style>
        <div class="pageContentDiv">
            <div hidden="{{'user' !== menuType}}" class="horizontal layout center-justified" style="font-size: 0.9em;">
                <button on-click="selectRepresentative">
                    <i class="fa fa-hand-o-right"></i> ${msg.saveAsRepresentativeLbl}
                </button>
            </div>
            <div class="text-center" style="margin:20px auto 15px 15px;">
                <div class="layout horizontal center center-justified" style="width:100%;">
                    <div hidden="{{!fabVisible}}">
                        <paper-fab mini icon="arrow-back" on-click="back" style="color: white;background:#ba0011;"></paper-fab>
                    </div>
                    <div data-representative-id$="{{representative.id}}" class="flex representativeNameHeader">
                        <div>{{representativeFullName}}</div>
                    </div>
                    <div class="representativeNumRepHeader">
                        <span>{{representative.numRepresentations}}</span> ${msg.numDelegationsPartMsg}
                    </div>
                </div>
            </div>
            <div style="margin:0px auto 0px auto;">

                <div class="horizontal layout" hidden="{{!smallScreen}}" style="margin: 20px 0 0 0;">
                    <div id="profileDiv" on-click="setProfileView" class="tab">${msg.profileLbl}</div>
                    <div id="votingDiv"  on-click="setVotingHistoryView"  class="tab">${msg.votingHistoryLbl}</div>
                </div>

                <div hidden="{{votingTabSelected}}" class="tabContent">
                    <div class="layout horizontal">
                        <div hidden="{{!representative.imageURL}}">
                            <img id="representativeImg" on-click="showImage"
                                 style="text-align:center; width: 100px;margin-right: 20px;"/>
                        </div>
                        <div style="margin:auto auto">
                            <vs-html-echo html="{{decodeBase64(representative.description)}}"></vs-html-echo>
                        </div>
                    </div>
                </div>

                <div hidden="{{!votingTabSelected}}" class="tabContent">
                    <div hidden="{{!isAdmin}}">
                        <div class="horizontal layout center center-justified" style="margin: 10px 0 0 0;">
                            <div>
                                <button id="votingHistoryButton" style="margin:0px 20px 0px 0px; width:300px;"
                                         on-click="requestVotingHistory">
                                    ${msg.requestVotingHistoryLbl}
                                </button>
                            </div>
                            <div>
                                <button id="accreditationRequestButton" style="margin:0px 20px 0px 0px; width:300px;"
                                        on-click="requestAccreditations">
                                    ${msg.requestRepresentativeAcreditationsLbl}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <image-viewer-dialog id="representativeImage" url="{{representative.imageURL}}" description="{{representativeFullName}}"></image-viewer-dialog>
        <representative-select-dialog id="selectRepresentativeDialog"></representative-select-dialog>
        <representative-request-accreditations-dialog id="accreditationsDialog"></representative-request-accreditations-dialog>
        <representative-request-votinghistory-dialog id="votingHistoryDialog"></representative-request-votinghistory-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-info',
            properties: {
                representative:{type:Object, value:{}, observer:'representativeChanged'},
                selectedTab:{type:String, value:'profile', observer:'selectedTabChanged'},
                isAdmin:{computed:'_checkIfAdmin(representative)'}
            },
            requestAccreditations:function(){
                this.$.accreditationsDialog.show(this.representative)
            },
            requestVotingHistory:function() {
                this.$.votingHistoryDialog.show(this.representative)
            },
            _checkIfAdmin:function(representative) {
                return 'admin' === menuType
            },
            setProfileView:function() {
                this.selectedTab = 'profile'
            },
            setVotingHistoryView:function() {
                this.selectedTab = 'votingHistory'
            },
            selectedTabChanged:function() {
                console.log(this.tagName + " selectedTabChanged - selectedTab: " + this.selectedTab)
                this.votingTabSelected = (this.selectedTab === 'votingHistory')
                if( this.selectedTab === 'votingHistory') {
                    this.$.votingDiv.className = 'tab tabSelected'
                    this.$.profileDiv.className = 'tab'
                } else {
                    this.$.profileDiv.className = 'tab tabSelected'
                    this.$.votingDiv.className = 'tab'
                }
            },
            decodeBase64:function(base64EncodedString) {
                if(base64EncodedString == null) return null
                return decodeURIComponent(escape(window.atob(base64EncodedString)))
            },
            selectRepresentative:function() {
                console.log("selectRepresentative")
                this.$.selectRepresentativeDialog.show(this.representative)
            },
            representativeChanged: function() {
                console.log(this.tagName + ".representativeChanged - selectedTab: " + this.selectedTab +
                " - img: " + this.representative.imageURL)
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
                if(this.representative.imageURL != null) this.$.representativeImg.src = this.representative.imageURL
            },
            ready: function() {
                this.selectedTab = 'profile'
                console.log(this.tagName + " - selectedTab: " + this.selectedTab)
            },
            showImage:function() {
                console.log(this.tagName + " - showImage")
                this.$.representativeImage.show()
            },
            back:function() {
                this.fire('representative-closed');
            }
        });
    </script>
</dom-module>