<%@ page contentType="text/html; charset=UTF-8" %>

<link href="./uservs-selector.vsp" rel="import"/>

<dom-module name="uservs-selector-dialog">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click>
            <div id="main">
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{msg.userSearchLbl}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <uservs-selector id="userVSSelector" groupvs-id="{{groupId}}" contact-selector="false"></uservs-selector>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'uservs-selector-dialog',
            properties :{
                groupvsId:{type:Object}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                document.querySelector("#voting_system_page").addEventListener('user-clicked', function(e) {
                    this.$.xDialog.opened = false
                }.bind(this))
            },
            show: function(groupVSId) {
                console.log(this.tagName + " - show - groupVSId: " + groupVSId)
                this.$.userVSSelector.groupVSId = groupVSId
                this.$.xDialog.opened = true
            },
            close: function() {
                this.$.userVSSelector.reset()
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>