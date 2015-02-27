<vs:webresource dir="polymer" file="polymer.html"/>
<vs:webresource dir="core-icon-button" file="core-icon-button.html"/>
<vs:webresource dir="vs-html-echo" file="vs-html-echo.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog.html"/>
<vs:webresource dir="paper-dialog" file="paper-dialog-transition.html"/>
<vs:webresource dir="core-signals" file="core-signals.html"/>

<polymer-element name="cooin-request-result-dialog">
    <template>
        <g:include view="/include/styles.gsp"/>
        <paper-dialog id="xDialog" layered backdrop class="votingsystemMessageDialog" on-core-overlay-open="{{onCoreOverlayOpen}}">
            <style no-shim>
            .votingsystemMessageDialog {
                top: 150px;
                box-sizing: border-box; -moz-box-sizing: border-box; font-family: Arial, Helvetica, sans-serif;
                font-size: 13px; overflow: auto; background: #f9f9f9; max-width: 500px; width: 400px;
                outline: 1px solid rgba(0,0,0,0.2);
                box-shadow: 0 4px 16px rgba(0,0,0,0.2);
            }
            </style>
            <core-signals on-core-signal-vs-wallet-save="{{walletSaved}}"></core-signals>
            <div>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;display:{{caption? 'block':'none'}}">{{caption}}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="font-size: 1.2em; color:#888; font-weight: bold; text-align: center; padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <vs-html-echo html="{{message}}"></vs-html-echo>
                </div>
                <div hidden?="{{isStoredInWallet}}" layout horizontal style="margin:0px 20px 0px 0px;">
                    <div flex></div>
                    <div>
                        <paper-button raised on-click="{{saveToSecureWallet}}">
                            <i class="fa fa-money"></i> <g:message code="saveToSecureWalletMsg"/>
                        </paper-button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer('cooin-request-result-dialog', {
            isStoredInWallet:false,
            ready: function() { },
            walletSaved: function() {
                this.isStoredInWallet = true;
            },
            saveToSecureWallet: function() {
                var webAppMessage = new WebAppMessage(Operation.WALLET_SAVE)
                webAppMessage.setCallback(function(appMessage) {
                    var appMessageJSON = JSON.parse(appMessage)
                    if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                        this.loadWallet(appMessageJSON.message)
                    } else {
                        var caption = '<g:message code="errorLbl"/>'
                        showMessageVS(appMessageJSON.message, caption)
                    }
                }.bind(this))
                VotingSystemClient.setJSONMessageToSignatureClient(webAppMessage);
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
</polymer-element>
