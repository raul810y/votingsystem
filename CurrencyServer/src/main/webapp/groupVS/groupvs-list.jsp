<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./groupvs-details.vsp" rel="import"/>

<dom-module name="groupvs-list">
    <template>
        <style></style>
        <link href="${contextURL}/resources/css/groupvs.css" media="all" rel="stylesheet" />
        <iron-ajax auto id="ajax" url="{{url}}" handle-as="json" content-type="application/json" last-response="{{groupListDto}}"></iron-ajax>
        <iron-signals on-iron-signal-groupvs-details-closed="closeGroupDetails"></iron-signals>
        <div hidden="{{groupListHidden}}" id="groupListPage">
            <div class="layout horizontal center center-justified" style="margin:0 0 10px 0;">
                <select id="groupvsTypeSelect" style="font-size: 1.3em; height: 30px; max-width: 400px;"
                        on-change="groupvsTypeSelect" class="form-control">
                    <option value="ACTIVE"  style="color:#388746;"> ${msg.selectActiveGroupvsLbl} </option>
                    <option value="PENDING" style="color:#fba131;"> ${msg.selectPendingGroupvsLbl} </option>
                    <option value="CANCELED" style="color:#cc1606;"> ${msg.selectClosedGroupvsLbl} </option>
                </select>
            </div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{groupListDto.resultList}}" as="groupvs">
                    <div on-tap="showGroupDetails" class$="{{groupvsClass(groupvs.state)}}" style="height: 65px;">
                        <div class='groupvsSubjectDiv'>{{groupvs.name}}</div>
                        <div hidden="{{!isItemCanceled(groupvs)}}" style="position: relative;">
                            <div class='groupvsMessageCancelled'>${msg.groupvsCancelledLbl}</div>
                        </div>
                        <div class='numTotalUsersDiv text-right'><span>{{groupvs.numActiveUsers}}</span> ${msg.usersLbl}</div>
                        <div class="horizontal layout">
                            <div class='groupvsRepresentativeDiv text-right'>{{getRepresentativeName(groupvs)}}</div>
                            <div class="flex"></div>
                            <template is="dom-repeat" items="{{groupvs.tags}}" as="tag">
                                <a class="btn btn-default" style="font-size: 0.6em;margin:0px 5px 0px 0px;padding:3px;">
                                    <i class="fa fa-tag" style="color:#888; margin: 0 5px 0 0;"></i><span>{{tag.name}}</span></a>
                            </template>
                        </div>
                    </div>
                </template>
            </div>

            <vs-pager on-pager-change="pagerChange" max="{{groupListDto.max}}"
                      next="${msg.nextLbl}" previous="${msg.previousLbl}"
                      first="${msg.firstLbl}" last="${msg.lastLbl}"
                      offset="{{groupListDto.offset}}" total="{{groupListDto.totalCount}}"></vs-pager>
        </div>
        <div  hidden="{{groupDetailsHidden}}">
            <groupvs-details id="groupDetails" fab-visible="true"></groupvs-details>
        </div>
    </template>
    <script>
        Polymer({
            is:'groupvs-list',
            properties: {
                groupListDto:{type:Object, value:{}, observer:'groupListDtoChanged'},
                url:{type:String, value:contextURL + "/rest/groupVS",observer:'urlChanged'}
            },
            decodeBase64:function(base64EncodedString) {
                try {
                    if(base64EncodedString == null) return null
                    var result = decodeURIComponent(escape(window.atob(base64EncodedString)))
                    return result.substring(0, 200) + "...";
                } catch (e) {}
            },
            urlChanged:function() { },
            groupListDtoChanged:function() {
                console.log(this.tagName + " - groupListDtoChanged")
                this.loading = false
            },
            ready :  function(e) {
                this.state = getURLParam("state")
                console.log(this.tagName + " - ready - state: " + this.state + " - groupListDto: " + this.groupListDto)
                if(this.state) this.$.groupvsTypeSelect.value = this.state
                if(this.groupListDto) this.groupListDtoChanged()
                this.groupListHidden = false
                this.groupDetailsHidden = true

                this.loading = true
            },
            isTaggedGroup:function(groupvs) {
                return (groupvs.tags && groupvs.tags.length > 0)
            },
            isItemCanceled:function(item) {
                return item.state === 'CANCELED'
            },
            pagerChange:function(e) {
                var optionSelected = this.$.groupvsTypeSelect.value
                targetURL = contextURL + "/rest/groupVS?menu=" + menuType + "&state=" +
                        optionSelected + "&max=" + e.detail.max + "&offset=" + e.detail.offset
                console.log(this.tagName + " - pagerChange - targetURL: " + targetURL)
                history.pushState(null, null, targetURL);
                this.$.ajax.url = targetURL
                console.log(this.tagName + "targetURL: " + targetURL)
                this.$.ajax.generateRequest()
            },
            closeGroupDetails:function(e, detail, sender) {
                console.log(this.tagName + " - closeGroupDetails")
                this.groupListHidden = false
                this.groupDetailsHidden = true
            },
            showGroupDetails :  function(e, details) {
                console.log(this.tagName + " - showGroupDetails")
                this.$.groupDetails.groupvs = e.model.groupvs;
                this.groupListHidden = !this.groupListHidden
                this.groupDetailsHidden = !this.groupDetailsHidden
            },
            getRepresentativeName:function(groupvs) {
                return groupvs.representative.firstName + " " + groupvs.representative.lastName
            },
            groupvsClass:function(state) {
                switch (state) {
                    case 'ACTIVE': return "groupvsDiv groupvs groupvsActive"
                    case 'PENDING': return "groupvsDiv groupvs groupvsPending"
                    case 'CANCELED': return "groupvsDiv groupvs groupvsFinished"
                }
            },
            groupvsTypeSelect: function() {
                var optionSelected = this.$.groupvsTypeSelect.value
                if("" != optionSelected) {
                    targetURL = contextURL + "/rest/groupVS?menu=" + menuType + "&state=" + optionSelected
                    history.pushState(null, null, targetURL);
                    this.$.ajax.url = targetURL
                    this.$.ajax.generateRequest()
                    console.log(this.tagName + " - groupvsTypeSelect: " + targetURL)
                }
            },
            processSearch:function (textToSearch) {
                app.updateSearchMessage("${msg.searchResultLbl} '" + textToSearch + "'")
                this.url = contextURL + "/rest/search/groupVS?searchText=" + textToSearch
            }
        });
    </script>
</dom-module>