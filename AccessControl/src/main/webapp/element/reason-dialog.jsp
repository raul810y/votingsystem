<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="reason-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <div id="container" layout vertical>
                <div class="layout horizontal center center-justified">
                    <div hidden="{{!caption}}" flex style="font-size: 1.4em; font-weight: bold; color:#6c0404;">
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
                    <textarea id="reason" rows="8" required="" style="width: 400px;"></textarea>
                </div>

                <div class="layout horizontal" style="margin:10px 20px 0px 0px; margin:10px;">
                    <div class="flex"></div>
                    <button on-click="submitForm">
                        <i class="fa fa-check" style="color: #388746;"></i> ${msg.acceptLbl}
                    </button>
                </div>
            </div>
        </div>
    </template>
<script>
    Polymer({
        is:'reason-dialog',
        properties: {
            caption:{type:String}
        },
        ready: function() { },
        submitForm: function() {
            this.fire('on-submit', this.$.reason.value);
            this.close()
        },
        show: function(message) {
            this.$.modalDialog.style.opacity = 1
            this.$.modalDialog.style['pointer-events'] = 'auto'
            this.messageToUser = message;
            this.$.reason.value = ""
        },
        close: function() {
            this.$.modalDialog.style.opacity = 0
            this.$.modalDialog.style['pointer-events'] = 'none'
        }
    });
</script>
</dom-module>
