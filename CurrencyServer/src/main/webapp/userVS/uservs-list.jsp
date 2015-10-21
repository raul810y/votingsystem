<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/bower_components/vs-pager/vs-pager.html" rel="import"/>
<link href="./uservs-subscription-card.vsp" rel="import"/>

<dom-module name="uservs-list">
    <template>
        <iron-signals on-iron-signal-refresh-uservs-list="refreshList"></iron-signals>
        <iron-ajax auto id="ajax" url="{{url}}" last-response="{{userListDto}}" handle-as="json"
                   content-type="application/json"></iron-ajax>
        <div style="margin: 0px auto 0px auto; max-width: 1200px; overflow:auto;">
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{userListDto.resultList}}" as="subscription">
                    <uservs-subscription-card subscription="{{subscription}}"></uservs-subscription-card>
                </template>
            </div>
            <vs-pager on-pager-change="pagerChange" max="{{userListDto.max}}"
                      next="${msg.nextLbl}" previous="${msg.previousLbl}"
                      first="${msg.firstLbl}" last="${msg.lastLbl}"
                      offset="{{userListDto.offset}}" total="{{userListDto.totalCount}}"></vs-pager>
        </div>
    </template>
    <script>
        Polymer({
            is:'uservs-list',
            properties: {
                userListDto:{type:Object, observer:'userListDtoChanged'},
                groupvsId:{type:Number},
                url:{type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            loadGroupUsers:function(groupvsId) {
                this.groupvsId = groupvsId
                console.log(this.tagName + " - loadGroupUsers - groupvsId: " + this.groupvsId)
                this.url = restURL + "/groupVS/id/" + this.groupvsId + "/listUsers"
            },
            refreshList: function(state) {
                this.$.ajax.generateRequest()
            },
            urlChanged:function() {
                console.log(this.tagName + " - urlChanged: " + this.url)
            },
            userListDtoChanged:function() {
                console.log(this.tagName + " - ready - userListDto.size: " + this.userListDto.resultList.length)
            },
            pagerChange: function(e) {
                var newURL = setURLParameter(this.url, "offset",  e.detail.offset)
                this.$.ajax.url = setURLParameter(newURL, "max", e.detail.max)
                console.log(this.tagName + " - pagerChange - newURL: " + this.$.ajax.url)
            }
        });
    </script>
</dom-module>