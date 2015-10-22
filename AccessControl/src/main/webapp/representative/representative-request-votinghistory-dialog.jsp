<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="representative-request-votinghistory-dialog">
    <template>
        <paper-dialog id="xDialog" style="width: 550px;" with-backdrop no-cancel-on-outside-click>
            <style></style>
            <div>
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">
                            ${msg.requestVotingHistoryLbl}
                        </div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>${msg.representativeHistoryRequestMsg}</div>
                <div class="layout horizontal">
                    <div class="flex"></div>
                    <div>
                        <button on-click="submit">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'representative-request-votinghistory-dialog',
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            show: function(representative) {
                this.$.xDialog.opened = true
                this.representative = representative
                this.representativeFullName = this.representative.firstName + " " + this.representative.lastName
            },
            submit: function() {
                var operationVS = new OperationVS(Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST)
                operationVS.jsonStr = JSON.stringify({operation:Operation.REPRESENTATIVE_VOTING_HISTORY_REQUEST,
                    representativeNif:this.representative.nif, representativeName:this.representativeFullName})
                operationVS.serviceURL = contextURL + "/rest/representative/history"
                operationVS.signedMessageSubject = '${msg.requestVotingHistoryLbl}'
                VotingSystemClient.setMessage(operationVS);
                this.close()
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>