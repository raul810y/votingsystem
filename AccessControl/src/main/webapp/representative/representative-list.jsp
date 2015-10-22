<%@ page contentType="text/html; charset=UTF-8" %>

<link href="representative-info.vsp" rel="import"/>
<link href="representation-state.vsp" rel="import"/>

<dom-module name="representative-list">
    <template>
        <style>
        .representativeDiv {
            width:300px;
            background-color: #f2f2f2;
            border: 1px solid #6c0404;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            margin: 10px 15px 10px 0px;
            -moz-border-radius: 5px;
            border-radius: 5px;
            cursor: pointer;
            height:90px;
            text-overflow: ellipsis;
        }
        </style>
        <iron-ajax auto url="{{url}}" last-response="{{representativeListDto}}" handle-as="json"
                   content-type="application/json"></iron-ajax>
        <div hidden="{{!detailsHidden}}">
            <div hidden="{{!representationInfo}}" class="linkVS" on-click="showRepresentativeListDto"
                 style="margin: 10px 0 10px 0; text-align: center;" >${msg.userRepresentativeLbl}</div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{representativeListDto.resultList}}">
                    <div on-tap="showRepresentativeDetails" class='representativeDiv horizontal layout center center center-justified'>
                        <div style=' max-width: 90px;'>
                            <img src='{{item.imageURL}}'/>
                        </div>
                        <div class='flex vertical layout center center center-justified'>
                            <p style="text-overflow: ellipsis; font-weight: bold;"><span>{{item.name}}</span></p>
                            <div style='margin: 10px 10px 0px 10px;'>
                                <span>{{item.numRepresentations}}</span> ${msg.numDelegationsPartMsg}
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </div>

        <div hidden="{{detailsHidden}}">
            <representative-info id="representativeDetails" fab-visible="true" on-representative-closed="closeRepresentativeDetails">
                </representative-info>
        </div>
        <representation-state id="representationState"></representation-state>
    </template>
    <script>
        Polymer({
            is:'representative-list',
            properties: {
                representativeListDto:{type:Object},
                url:{type:String, value: contextURL + "/rest/representative"}
            },
            ready : function(e) {
                console.log(this.tagName + " - ready")
                this.detailsHidden = true
                if(this.$.representationState.hasData === true) this.representationInfo = true
                else this.representationInfo = false
            },
            closeRepresentativeDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeRepresentativeDetails")
                this.detailsHidden = true
            },
            showRepresentativeListDto : function() {
                this.$.representationState.show()
            },
            showRepresentativeDetails :  function(e) {
                console.log(this.tagName + " - showRepresentativeDetails")
                this.$.representativeDetails.representative = e.model.item;
                this.detailsHidden = false
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            getSubject:function(eventSubject) {
                return eventSubject.substring(0,50) + ((eventSubject.length > 50)? "...":"");
            }
        });
    </script>
</dom-module>
