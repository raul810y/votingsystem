<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-input', file: 'paper-input.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-dialog', file: 'paper-dialog-transition.html')}">


<polymer-element name="eventvs-option-dialog" attributes="opened">
    <template>
        <style></style>
        <paper-dialog id="xDialog" layered backdrop opened="{{opened}}" layered="true" sizingTarget="{{$.container}}">
            <g:include view="/include/styles.gsp"/>
            <!-- place all overlay styles inside the overlay target -->
            <style no-shim>
                .messageToUser {
                    font-weight: bold; margin:10px auto 10px auto; color:#ba0011; padding:10px 20px 10px 20px;
                }
            </style>
            <div id="container" layout vertical>
                <div layout horizontal center center-justified>
                    <div flex style="font-size: 1.5em; margin:0px 0px 0px 30px;font-weight: bold; color:#6c0404;">
                        <div style="text-align: center;"><g:message code="addOptionLbl"/></div>
                    </div>
                    <div style="position: absolute; top: 0px; right: 0px;">
                        <core-icon-button on-click="{{close}}" icon="close" style="fill:#6c0404; color:#6c0404;"></core-icon-button>
                    </div>
                </div>
                <div style="display:{{messageToUser? 'block':'none'}}">
                    <div class="messageToUser">
                        <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                            <div id="messageToUser">{{messageToUser}}</div>
                        </div>
                        <paper-shadow z="1"></paper-shadow>
                    </div>
                </div>
                <div  layout horizontal center center-justified  style="margin:10px 0px 10px 0px;">
                    <input type="text" class="form-control" id="optionContent" style="width:420px;"
                           placeholder="<g:message code="pollOptionContentMsg"/>" required/>
                </div>
                <div layout horizontal style="margin:10px 20px 0px 0px; margin:10px;">
                    <div flex></div>
                    <paper-button raised on-click="{{submitForm}}" style="margin: 0px 0px 0px 5px;">
                        <i class="fa fa-check"></i> <g:message code="acceptLbl"/>
                    </paper-button>
                </div>
            </div>
        </paper-dialog>
    </template>
<script>
    Polymer('eventvs-option-dialog', {
        opened: false,
        ready: function() {
            this.$.optionContent.onkeypress = function(event){
                if (event.keyCode == 13) this.submitForm()
            }.bind(this)
        },
        openedChanged: function() {
            if(!this.opened) {
                this.messageToUser = null
                this.$.optionContent.value = ""
            }
        },
        close: function() {
            this.opened = false;
        },
        submitForm: function() {
            this.messageToUser = null
            if(!this.$.optionContent.validity.valid) {
                this.messageToUser = "<g:message code="emptyFieldMsg"/>"
                return
            }
            this.fire('on-submit', this.$.optionContent.value);
            this.opened = false
        },
        toggle: function() {
            this.$.xDialog.toggle();
        },
        show: function() {
            this.$.xDialog.opened = true
        }
    });
</script>

</polymer-element>