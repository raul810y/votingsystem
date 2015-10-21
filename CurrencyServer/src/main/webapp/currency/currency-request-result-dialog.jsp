<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="currency-request-result-dialog">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click>
            <style> </style>
            <iron-signals on-iron-signal-vs-wallet-save="walletSaved"></iron-signals>
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div hidden="{{!caption}}" style="text-align: center;">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div style="font-size: 1.3em; color:#888; font-weight: bold; text-align: center;
                        padding:20px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <vs-html-echo html="{{message}}"></vs-html-echo>
                </div>
                <div hidden="{{isStoredInWallet}}" class="horizontal layout flex" style="margin:10px 20px 0px 0px;">
                    <button on-click="saveToSecureWallet">
                        <i class="fa fa-money"></i> ${msg.saveToSecureWalletMsg}
                    </button>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'currency-request-result-dialog',
            isStoredInWallet:false,
            ready: function() { },
            walletSaved: function() {
                this.isStoredInWallet = true;
            },
            saveToSecureWallet: function() {
                var operationVS = new OperationVS(Operation.WALLET_SAVE)
                operationVS.setCallback(function(appMessage) { this.saveResponse(appMessage)}.bind(this))
                VotingSystemClient.setMessage(operationVS);
            },
            saveResponse:function(appMessage) {
                var appMessageJSON = JSON.parse(appMessage)
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    this.loadWallet(appMessageJSON.message)
                } else {
                    var caption = '${msg.errorLbl}'
                    showMessageVS(appMessageJSON.message, caption)
                }
                this.click()
            },
            showMessage:function(caption, message) {
                this.caption = caption;
                this.message = message;
                this.isStoredInWallet = false
                this.$.xDialog.opened = true
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>
