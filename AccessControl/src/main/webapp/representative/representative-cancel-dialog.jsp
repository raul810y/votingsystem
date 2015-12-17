<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-cancel-dialog">
    <template>
        <div id="modalDialog" class="modalDialog">
            <style>
                .textDialog {
                    font-size: 1.2em; color:#888; font-weight: bold; text-align: center;
                }
            </style>
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            ${msg.removeRepresentativeLbl}
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div class="textDialog" style="padding:10px 20px 10px 20px; display:block;word-wrap:break-word;">
                    <p style="text-align: center;">${msg.removeRepresentativeMsg}</p>
                </div>
                <div  class="textDialog" layout vertical center center-justified style="margin:0px auto 0px auto;">
                    <label style="margin:0px 0px 20px 0px">${msg.representativeNIFLbl}</label>
                    <input type="text" id="representativeNif" style="width:150px; margin:0px auto 0px auto;" class="form-control"/>
                </div>
                <div class="layout horizontal" style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <div>
                        <button on-click="accept" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'representative-cancel-dialog',
            ready: function() {
                console.log(this.tagName + " - ready")
                this.$.representativeNif.onkeypress = function(event){
                    if (event.keyCode == 13) this.accept()
                }.bind(this)
            },
            show: function() {
                this.$.modalDialog.style.opacity = 1
                this.$.modalDialog.style['pointer-events'] = 'auto'
                this.$.representativeNif.value = ""
            },
            accept: function() {
                console.log(this.tagName + " - removeRepresentative")
                var validatedNif = validateNIF(this.$.representativeNif.value)
                if(validatedNif == null) {
                    alert('${msg.nifERRORMsg}','${msg.errorLbl}')
                    return;
                }
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_REVOKE)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_REVOKE, nif:validatedNif})
                operationVS.serviceURL = contextURL + "/rest/representative/revoke"
                operationVS.signedMessageSubject = '${msg.removeRepresentativeMsgSubject}'
                operationVS.setCallback(function(appMessage) { this.revokeResponse(appMessage) }.bind(this))
                VotingSystemClient.setMessage(operationVS);
                this.close()
            },
            revokeResponse: function(appMessage) {
                console.log(this.tagName + "revokeResponse - message: " + appMessage);
                var appMessageJSON = toJSON(appMessage)
                var caption = '${msg.operationERRORCaption}'
                var msg = appMessageJSON.message
                if(ResponseVS.SC_OK == appMessageJSON.statusCode) {
                    caption = "${msg.operationOKCaption}"
                    msg = "${msg.removeRepresentativeOKMsg}";
                } else if (ResponseVS.SC_CANCELED== appMessageJSON.statusCode) {
                    caption = "${msg.operationCANCELEDLbl}"
                }
                alert(msg, caption)
                this.click() //hack to refresh screen
            },
            close: function() {
                this.$.modalDialog.style.opacity = 0
                this.$.modalDialog.style['pointer-events'] = 'none'
            }
        });
    </script>
</dom-module>
