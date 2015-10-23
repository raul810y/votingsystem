<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="vs-cert">
    <template>
        <style>
        .certDiv {
            background-color: #fefefe;
            padding: 10px;
            height: 150px;
            -moz-border-radius: 5px; border-radius: 5px;
            overflow:hidden;
        }
        </style>
        <iron-ajax id="ajax" url="{{url}}" last-response="{{cert}}" handle-as="json" method="get" content-type="application/json"></iron-ajax>
        <div class="layout vertical center center-justified">
            <div style="width: 600px;">
                <div class="layout horizontal center center-justified" style="width: 100%;">
                    <div hidden="{{!fabVisible}}">
                        <paper-fab mini icon="arrow-back" on-click="back" style="color: white;"></paper-fab>
                    </div>
                    <div class="flex">
                        <h3>
                            <div id="pageHeaderDiv" class="pageHeader text-center"></div>
                        </h3>
                    </div>
                </div>

                <div class="certDiv">
                    <div>
                        <div class='groupvsSubjectDiv' style="display: inline;"><span style="font-weight: bold;">
                            ${msg.serialNumberLbl}: </span>{{certvs.serialNumber}}</div>
                        <div id="certStateDiv" style="display: inline; margin:0px 0px 0px 20px; font-size: 1.2em; font-weight: bold; float: right;"></div>
                    </div>
                    <div class='groupvsSubjectDiv'><span style="font-weight: bold;">${msg.subjectLbl}: </span>{{certvs.subjectDN}}</div>
                    <div><span style="font-weight: bold;">${msg.issuerLbl}: </span>
                        <a id="issuerURL" on-click="certIssuerClicked" style="cursor: pointer;">{{certvs.issuerDN}}</a>
                    </div>
                    <div><span style="font-weight: bold;">${msg.signatureAlgotithmLbl}: </span>{{certvs.sigAlgName}}</div>
                    <div>
                        <div style="display: inline;">
                            <span style="font-weight: bold;">${msg.noBeforeLbl}: </span>{{getDate(certvs.notBefore)}}</div>
                        <div style="display: inline; margin:0px 0px 0px 20px;">
                            <span style="font-weight: bold;">${msg.noAfterLbl}: </span>{{getDate(certvs.notAfter)}}</div>
                    </div>
                    <div hidden="{{!certvs.isRoot}}" class="text-center" style="font-weight: bold; display: inline;
                            margin:0px auto 0px auto;color: #6c0404; float:right; text-decoration: underline;">${msg.rootCertLbl}
                    </div>
                </div>
                <div style="width: 600px; max-height:400px; overflow-y: auto; margin:20px auto 0px auto;">
                    <div>{{certvs.description}}</div>
                </div>

                <div class="vertical layout center center-justified" style="width: 600px; margin:20px auto 0px auto;">
                    <div><label>${msg.certPublicKeyLbl}</label></div>
                    <div>
                        <textarea id="pemCertTextArea" style="width:520px; height:300px;font-family: monospace; font-size:0.8em;" readonly></textarea>
                    </div>
                </div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'vs-cert',
            properties: {
                fabVisible:{type:Boolean, value:false},
                certvs:{type:Object, value:{}, observer:'certvsChanged'}
            },
            certvsChanged: function() {
                this.$.pemCertTextArea.value = this.certvs.pemCert
                if('CERTIFICATE_AUTHORITY' == this.certvs.type) {
                    this.$.pageHeaderDiv.innerHTML = "${msg.trustedCertPageTitle}"
                } else if ('USER' == this.certvs.type) {
                    this.$.pageHeaderDiv.innerHTML = "${msg.userCertPageTitle}"
                }
                if('OK' == this.certvs.state) {
                    this.$.certStateDiv.innerHTML = "${msg.certOKLbl}"
                } else if ('CANCELED' == this.certvs.state) {
                    this.$.certStateDiv.innerHTML = "${msg.certCancelledLbl}"
                }
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            ready: function() {
                this.certsSelectedStack = []
            },
            back:function() {
                var previousCert = this.certsSelectedStack.pop()
                if(previousCert != null) {
                    this.url = ""
                    this.certvs = previousCert
                    if(this.certsSelectedStack.length == 0) this.subcert = null
                } else {
                    console.log(this.tagName + " - iron-signal-vs-cert-closed")
                    this.fire('cert-closed');
                }
            },
            certIssuerClicked:function(e) {
                var issuerSerialNumber = e.model.item.issuerSerialNumber
                if(issuerSerialNumber != null) {
                    var certURL = contextURL + "/rest/certificateVS/serialNumber/" + issuerSerialNumber
                    console.log(this.tagName + " - certIssuerClicked: " + certURL)
                    this.certsSelectedStack.push(this.certvs)
                    this.url = certURL
                    this.subcert = true
                }
            }
        })
    </script>
</dom-module>