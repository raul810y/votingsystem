<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="paper-fab" file="paper-fab.html"/>
<vs:webresource dir="paper-tabs" file="paper-tabs.html"/>
<vs:webcomponent path="/element/image-viewer-dialog"/>
<vs:webcomponent path="/representative/representative-select-dialog"/>
<vs:webcomponent path="/representative/representative-request-accreditations-dialog"/>
<vs:webcomponent path="/representative/representative-request-votinghistory-dialog"/>

<polymer-element name="representative-info" attributes="subpage">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style>
            .tabContent {
                margin:0px auto 0px auto;
                width:auto;
            }
            paper-tabs.transparent-teal {
                background-color: #ffeeee;
                color:#ba0011;
                box-shadow: none;
                cursor: pointer;
            }
            paper-tabs.transparent-teal::shadow #selectionBar {
                background-color: #ba0011;
            }
            paper-tabs.transparent-teal paper-tab::shadow #ink {
                color: #ba0011;
            }
        </style>
        <div class="pageContentDiv">
            <div style="margin: 0 30px 0 30px;">
                <div class="text-center row" style="margin:20px auto 15px 15px;">
                    <div layout horizontal center center-justified style="width:100%;">
                        <template if="{{subpage}}">
                            <paper-fab icon="arrow-back" on-click="{{back}}" style="color: white;"></paper-fab>
                        </template>
                        <div flex representativeId-data="{{representative.id}}" class="representativeNameHeader">
                            <div>{{representativeFullName}}</div>
                        </div>
                    </div>
                    <div  class="representativeNumRepHeader" style="">
                        {{representative.numRepresentations}} <g:message code="numDelegationsPartMsg"/>
                    </div>
                    <template if="{{'user' == menuType}}">
                        <div>
                            <paper-button raised on-click="{{selectRepresentative}}" style="margin:15px 20px 15px 0px;">
                                <i class="fa fa-hand-o-right"></i> <g:message code="saveAsRepresentativeLbl"/>
                            </paper-button>
                        </div>
                    </template>
                </div>
                <div  style="width: 1000px; margin:0px auto 0px auto;">
                    <paper-tabs style="width: 1000px;margin:0px auto 0px auto;" class="transparent-teal center" valueattr="name"
                                 selected="{{selectedTab}}"  on-core-select="{{tabSelected}}" noink>
                        <paper-tab name="profile" style="width: 400px"><g:message code="profileLbl"/></paper-tab>
                        <paper-tab name="votingHistory"><g:message code="votingHistoryLbl"/></paper-tab>
                    </paper-tabs>
                    <div class="tabContent" style="display:{{selectedTab == 'profile'?'block':'none'}}">
                        <div layout horizontal>
                            <div>
                                <img id="representativeImg" on-click="{{showImage}}" src="{{representative.imageURL}}"
                                     style="text-align:center; width: 100px;margin-right: 20px;"></img>
                            </div>
                            <div style="margin:auto auto">
                                <vs-html-echo html="{{representative.description}}"></vs-html-echo>
                            </div>
                        </div>
                    </div>

                    <div class="tabContent" style="display:{{selectedTab == 'votingHistory'?'block':'none'}}">
                        <template if="{{'admin' == menuType}}">
                            <div style="margin: auto;top: 0; left: 0; right: 0; position:relative;display:table;">
                                <div style="display:table-cell;">
                                    <paper-button raised id="votingHistoryButton" style="margin:0px 20px 0px 0px; width:300px;"
                                             on-click="{{requestVotingHistory}}">
                                        <g:message code="requestVotingHistoryLbl"/>
                                    </paper-button>
                                </div>
                                <div style="display:table-cell;">
                                    <paper-button raised id="accreditationRequestButton" style="margin:0px 20px 0px 0px; width:300px;"
                                            on-click="{{requestAccreditations}}">
                                        <g:message code="requestRepresentativeAcreditationsLbl"/>
                                    </paper-button>
                                </div>
                            </div>
                        </template>
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
        Polymer('representative-info', {
            menuType:menuType,
            subpage:false,
            publish: {
                representative: {value: {}}
            },
            requestAccreditations:function(){
                this.$.accreditationsDialog.show(this.representative)
            },
            requestVotingHistory:function() {
                this.$.votingHistoryDialog.show(this.representative)
            },
            selectRepresentative:function() {
                console.log("selectRepresentative")
                this.$.selectRepresentativeDialog.show(this.representative)
            },
            representativeChanged: function() {
                console.log(this.tagName + ".representativeChanged - subpage:  " + this.subpage + " - selectedTab: " + this.selectedTab)
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
            },
            ready: function() {
                this.selectedTab = 'profile'
                console.log(this.tagName + " - subpage:  " + this.subpage + " - selectedTab: " + this.selectedTab)
            },
            showImage:function() {
                console.log(this.tagName + " - showImage")
                this.$.representativeImage.show()
            },
            back:function() {
                this.fire('core-signal', {name: "representative-closed", data: null});
            }
        });
    </script>
</polymer-element>