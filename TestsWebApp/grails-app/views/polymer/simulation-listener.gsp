<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/votingsystem-socket', file: 'votingsystem-socket.html')}">

<polymer-element name="simulation-listener" attributes="subpage pagetitle">
    <template>
        <g:include view="/include/styles.gsp"/>
        <style> </style>
        <div layout vertical center>

            <div layout horizontal center center-justified style="width:100%;">
                <template if="{{subpage}}">
                    <votingsystem-button isFab on-click="{{back}}" style="font-size: 1.5em; margin:5px 0px 0px 0px;">
                        <i class="fa fa-arrow-left"></i></votingsystem-button>
                </template>
                <div flex id="pageTitle" class="pageHeader" style="text-align: center;"><h3>{{pagetitle}}</h3></div>
                <votingsystem-socket id="socketvs" socketservice="${grailsApplication.config.grails.socketServiceURL}">
                    <button onclick="{{listenClaimSimulationService}}">listenClaimSimulationService</button>
                </votingsystem-socket>
            </div>

            <div id="progressDiv" style="width:100%;height:100%;display:{{isProcessing? 'block':'none'}}">
                <progress style="margin:20px auto 20px auto;"></progress>
            </div>

            <div id="messageFromService" class="messageFromServiceBox" style="display:{{message != null? 'block':'none'}}"">

                <div style="margin: 0 0 20px 0;">
                    <div>
                        <label style="font-weight: bold;"><g:message code="statusCodelLbl"/>:</label>
                        <div style="margin: 0 20px 0 0;font-size: 2em;font-weight: bold;" id="statusCode">{{message.statusCode}}</div>
                    </div>
                    <div style="margin: 0 20px 0 0;">
                        <label style="font-weight: bold;"><g:message code="timDurationLbl"/>:</label>
                        <div style="font-family:digi;font-size: 2em;">{{message.simulationData.timeDuration}}</div>
                    </div>
                </div>
                <div id="messageDiv" style="margin: 0 0 15px 0;">
                    <label style="font-weight: bold;"><g:message code="messageLbl"/>:</label>
                    <div id="message">{{message.message}}</div>
                </div>
                <div id="numRequestsDiv" style="margin: 0 0 15px 0;">
                    <label style="font-weight: bold;"><g:message code="numRequestsLbl"/>:</label><label>{{message.simulationData.numRequestsProjected}}</label>
                </div>
                <div>
                    <div id="numRequestsOKDiv" style="width: 700px; margin: 0 50px 0 0;">
                        <label style="font-weight: bold;"><g:message code="numRequestsOKLbl"/>:</label><label>{{message.simulationData.numRequestsOK}}</label>
                    </div>
                    <div id="numRequestsERRORDiv" style="width: 400px;">
                        <label style="font-weight: bold;"><g:message code="numRequestsERRORLbl"/>:</label><label>{{message.simulationData.numRequestsERROR}}</label>
                    </div>
                </div>
                <div id="errorsDiv">
                    <label style="font-weight: bold;"><g:message code="errorsListLbl"/>:</label>
                    <div>{{message.simulationData.errorList}}</div>
                </div>

            </div>
        </div>
    </template>
    <script>
        Polymer('simulation-listener', {
            publish: {
                simulationData: {value: {}}
            },
            subpage:false,
            isProcessing:false,
            simulationDataChanged: function() {
                this.$.socketvs.sendMessage(JSON.stringify(this.simulationData))
            },
            ready: function() {
                console.log(this.tagName + " - ready - subpage: " + this.subpage +
                        " - socketvs service: ${grailsApplication.config.grails.socketServiceURL}")
                this.$.socketvs.addEventListener('on-message', function(e) {
                    console.log(this.tagName + " - socketvs - message: " + e.detail)
                    this.processSocketMessage(e.detail)
                }.bind(this));
            },
            back:function() {
                this.fire('core-signal', {name: "simulation-listener-closed", data: null});
            },
            submitClaim:function() {
                console.log("submitClaim")
            },
            processSocketMessage:function(socketMessage){
                this.message = socketMessage
                if(this.message.statusCode == ResponseVS.SC_PROCESSING) {
                    this.isProcessing = true
                }  else this.isProcessing = false
            },
            listenClaimSimulationService:function(){
                this.$.socketvs.sendMessage(JSON.stringify({service:"claimSimulationService", operation:"LISTEN"}))
            }

        });
    </script>
</polymer-element>