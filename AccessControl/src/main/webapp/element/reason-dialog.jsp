<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="reason-dialog">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click>
            <div id="container" layout vertical>
                <div class="layout horizontal center center-justified">
                    <div hidden="{{!caption}}" flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div hidden="{{!messageToUser}}" class="center" style="color: #6c0404;">
                    {{messageToUser}}
                </div>
                <div style="margin:20px 0px 10px 0px;">
                    <label>${msg.cancelSubscriptionFormMsg}</label>
                    <textarea id="reason" rows="8" required=""></textarea>
                </div>

                <div class="layout horizontal" style="margin:10px 20px 0px 0px; margin:10px;">
                    <div class="flex"></div>
                    <button on-click="submitForm">
                        <i class="fa fa-check" style="color: #388746;"></i> ${msg.acceptLbl}
                    </button>
                </div>
            </div>
        </paper-dialog>
    </template>
<script>
    Polymer({
        is:'reason-dialog',
        properties: {
            caption:{type:String}
        },
        ready: function() {
            this.opened = false
        },
        submitForm: function() {
            this.fire('on-submit', this.$.reason.value);
            this.close()
        },
        show: function(message) {
            this.$.xDialog.opened = true
            this.messageToUser = message;
            this.$.reason.value = ""
        },
        close: function() {
            this.$.xDialog.opened = false
        }
    });
</script>
</dom-module>
