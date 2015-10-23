<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="eventvs-election-vote-confirm-dialog">
    <template>
        <paper-dialog id="xDialog" with-backdrop no-cancel-on-outside-click>
            <style> </style>
            <div style="display: block;">
                <div class="layout horizontal center center-justified">
                    <div class="flex" style="font-size: 1.5em; margin:5px 0px 10px 10px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;">${msg.confirmOptionDialogCaption}</div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <i class="fa fa-times closeIcon" on-click="close"></i>
                    </div>
                </div>
                <div>
                    <p style="text-align: center;"> ${msg.confirmOptionDialogMsg}:<br>
                        <div style="font-size: 1.3em; text-align: center;"><b>{{optionSelected}}</b></div>
                    </p>
                </div>
                <div class="layout horizontal" style="margin:0px 20px 0px 0px;">
                    <div class="flex"></div>
                    <div>
                        <button on-click="optionConfirmed" style="margin: 0px 0px 0px 5px;">
                            <i class="fa fa-check"></i> ${msg.acceptLbl}
                        </button>
                    </div>
                </div>
            </div>
        </paper-dialog>
    </template>
    <script>
        Polymer({
            is:'eventvs-election-vote-confirm-dialog',
            ready: function() {
                console.log(this.tagName + " - ready - ")
            },
            show: function(optionSelected) {
                console.log(this.tagName + " - optionSelected: " + optionSelected)
                this.optionSelected = optionSelected
                this.$.xDialog.opened = true
            },
            optionConfirmed: function() {
                this.fire('option-confirmed', this.optionSelected);
                this.$.xDialog.opened = false
            },
            close: function() {
                this.$.xDialog.opened = false
            }
        });
    </script>
</dom-module>
