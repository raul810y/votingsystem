<link rel="import" href="${resource(dir: '/bower_components/polymer', file: 'polymer.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon-button', file: 'core-icon-button.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-transition', file: 'core-transition-css.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-ajax', file: 'core-ajax.html')}">
<link rel="import" href="${resource(dir: '/bower_components/core-icon', file: 'core-icon.html')}">
<link rel="import" href="${resource(dir: '/bower_components/paper-shadow', file: 'paper-shadow.html')}">
<link rel="import" href="${resource(dir: '/bower_components/google-chart', file: 'google-chart.html')}">
<link rel="import" href="<g:createLink  controller="polymer" params="[element: '/polymer/dialog/vicket-transactionvs-dialog']"/>">


<!-- an element that uses the votingsystem-dialog element and core-overlay -->
<polymer-element name="balance-details" attributes="url">
<template>
    <vicket-transactionvs-dialog id="transactionViewer"></vicket-transactionvs-dialog>
        <!-- place all overlay styles inside the overlay target -->
        <style no-shim>
        .dialog {
            box-sizing: border-box;
            -moz-box-sizing: border-box;
            font-family: Arial, Helvetica, sans-serif;
            font-size: 13px;
            -webkit-user-select: none;
            -moz-user-select: none;
            overflow: auto;
            background: white;
            padding:10px 30px 30px 30px;
            outline: 1px solid rgba(0,0,0,0.2);
            box-shadow: 0 4px 16px rgba(0,0,0,0.2);
            width: 820px;
        }
        .messageToUser {
            font-weight: bold;
            margin:10px auto 10px auto;
            border: 1px solid #ccc;
            background: #f9f9f9;
            padding:10px 20px 10px 20px;
        }
        .subjectColumn {
            width:220px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px; cursor: pointer;
        }
        .amountColumn {width:120px;text-align: right; }
        .tagColumn {font-size: 0.6em; text-align: center; vertical-align: middle; width: 100px; text-overflow: ellipsis;}
        </style>

        <core-ajax auto id="ajax" handleAs="json" response="{{balance}}" contentType="json"
                   on-core-response="{{ajaxResponse}}" on-core-error="{{ajaxError}}"></core-ajax>
        <div layout vertical style="" >

            <div layout horizontal center center-justified style="" >
                <div id="caption" flex style="color: #6c0404; font-weight: bold; font-size: 1.2em; text-align: center;
                    margin:10px 0 10px 0;">
                    {{caption}} - {{balance.name}}</div>
            </div>
            <div style="text-align: center">
                <g:message code="periodLbl"/>: {{balance.timePeriod.dateFrom}} -- {{balance.timePeriod.dateTo}}
            </div>

            <div style="font-size: 0.8em; color: #888;">{{description}}</div>
            <template if="{{status}}">
                <div class="messageToUser" style="color: {{message.status == 200?'#388746':'#ba0011'}};">
                    <div  layout horizontal center center-justified style="margin:0px 10px 0px 0px;">
                        <div id="messageToUser">{{message}}</div>
                        <core-icon icon="{{status == 200?'check':'error'}}" style="fill:{{message.status == 200?'#388746':'#ba0011'}};"></core-icon></div>

                    <paper-shadow z="1"></paper-shadow>
                </div>
            </template>
            <div style="display:{{balance.nif != null ? 'block':'none'}}">
                <div layout horizontal>
                <div style="display: {{balance.transactionToList.length > 0 ? 'block':'none'}}">
                    <div layout vertical style="border-right: 2px solid #6c0404; padding:0 30px 0 0; margin: 0 20px 0 0;">
                        <div style="font-weight: bold; color:#6c0404;"><g:message code="incomesLbl"/></div>
                        <div layout vertical center center-justified>
                            <div>
                                <template repeat="{{transaction in balance.transactionToList}}">
                                    <div layout horizontal on-click="{{viewTransaction}}">
                                        <div layout vertical center center-justified class="tagColumn">{{transaction.tags[0].name}}</div>
                                        <div class="subjectColumn" style="">{{transaction.subject}}</div>
                                        <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                                    </div>
                                </template>
                                <div layout horizontal>
                                    <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                                        <g:message code="totalLbl"/>: </div>
                                    <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionToTotal}}</div>
                                </div>
                            </div>
                            <div style="margin:0px 0px 0px 20px;">
                                <google-chart id='balanceToChart' type='pie' height='200px' width='250px'
                                              options='{"title": "<g:message code="tagsLbl"/>", "pieHole": 0.4, "chartArea":{"left":"0","width":"100%"},
                              "legend":{"alignment":"center", "position":"right"}, "animation": {"duration": "1000"}}'
                                              cols='[{"label": "Data", "type": "string"},{"label": "Cantidad", "type": "number"}]'>
                                </google-chart>
                            </div>
                        </div>
                    </div>
                </div>
                <div style="display:{{balance.transactionFromList.length > 0 ? 'block':'none'}}">
                    <div layout vertical>
                        <div style="font-weight: bold;color:#6c0404;">{{expensesLbl}}</div>
                        <div layout vertical center center-justified>
                            <div>
                                <template repeat="{{transaction in balance.transactionFromList}}">
                                    <div layout horizontal on-click="{{viewTransaction}}">
                                        <div layout vertical center center-justified class="tagColumn">{{transaction.tags[0].name}}</div>
                                        <div class="subjectColumn" style="">{{transaction.subject}}</div>
                                        <div class="amountColumn">{{transaction.amount}} {{transaction.currency}}</div>
                                    </div>
                                </template>
                                <div layout horizontal>
                                    <div flex class="subjectColumn" style="text-align: right;font-weight: bold;">
                                        <g:message code="totalLbl"/>: </div>
                                    <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionFromTotal}}</div>
                                </div>
                            </div>
                            <div style="margin:0px 0px 0px 20px; max-width: 200px;">
                                <google-chart id='balanceFromChart' type='pie' height='200px' width='250px'
                                              options='{"title": "<g:message code="tagsLbl"/>", "pieHole": 0.4, "chartArea":{"left":"0","width":"100%"},
                              "legend":{"alignment":"center", "position":"right"}, "animation": {"duration": "1000"}}'
                                              cols='[{"label": "Data", "type": "string"},{"label": "Cantidad", "type": "number"}]'>
                                </google-chart>
                            </div>
                        </div>
                    </div>
                </div>
                </div>
            </div>
            </div>
        </div>
    </div>
</template>
<script>

    function addNumbers(num1, num2) {
        return (new Number(num1) + new Number(num2)).toFixed(2)
    }

    Polymer('balance-details', {
        publish: {
            balance: {value: {}}
        },
        ready: function(e) {
            console.log(this.tagName + " - ready")
        },
        viewTransaction: function(e) {
            console.log(this.tagName + " - viewTransaction")
            this.$.transactionViewer.transactionvs =  e.target.templateInstance.model.transaction;
        },
        balanceChanged: function() {
            console.log(this.tagName + " - balanceChanged - balance.type: " + this.balance.type)
            this.$.balanceToChart.rows = []
            this.$.balanceFromChart.rows = []
            var transactionTotal = 0
            var caption = "<g:message code="balanceDetailCaption"/>"
            var userType
            if('SYSTEM' == this.balance.type) {
                this.caption = caption.format("<g:message code="systemLbl"/>")
                this.description = "IBAN: " + this.balance.IBAN
                this.expensesLbl = "<g:message code="expensesLbl"/>"
            }
            else if('BANKVS' == this.balance.type) {
                this.caption = caption.format("<g:message code="bankVSLbl"/>")
                this.description = "NIF:" + this.balance.nif + " - IBAN: " + this.balance.IBAN
                this.expensesLbl = "<g:message code="contributionsLbl"/>"
            }
            else if('GROUP' == this.balance.type) {
                this.caption = caption.format("<g:message code="groupLbl"/>")
                this.description = "IBAN: " + this.balance.IBAN
                this.expensesLbl = "<g:message code="expensesLbl"/>"
                this.balance.nif = ""
            }
            else if('USER' == this.balance.type || this.balance.userVS) {
                this.balance.nif = this.balance.userVS.nif
                this.balance.name = this.balance.userVS.firstName + " " + this.balance.userVS.lastName
                this.caption = caption.format("<g:message code="userLbl"/>")
                this.description = "NIF:" + this.balance.nif + " - IBAN: " + this.balance.userVS.IBAN
                this.expensesLbl = "<g:message code="expensesLbl"/>"
            }


            if(this.balance.balancesTo && this.balance.balancesTo["EUR"]) { //incomes
                transactionTotal = 0
                Object.keys(this.balance.balancesTo["EUR"]).forEach(function(entry) {
                    transactionTotal = addNumbers(transactionTotal, this.balance.balancesTo["EUR"][entry])
                    var row = [entry, this.balance.balancesTo["EUR"][entry]]
                    this.$.balanceToChart.rows.push(row)
                }.bind(this))
                this.transactionToTotal = new Number(transactionTotal).toFixed(2) + " EUR"
            }

            if(this.balance.balancesFrom && this.balance.balancesFrom["EUR"]) {//expenses
                transactionTotal = 0
                Object.keys(this.balance.balancesFrom["EUR"]).forEach(function(entry) {
                    transactionTotal = addNumbers(transactionTotal, this.balance.balancesFrom["EUR"][entry])
                    var row = [entry, this.balance.balancesFrom["EUR"][entry]]
                    this.$.balanceFromChart.rows.push(row)
                }.bind(this))
                this.transactionFromTotal = new Number(transactionTotal).toFixed(2) + " EUR"
            }

        },
        tapHandler: function() {
            this.$.xDialog.toggle();
        },
        urlChanged: function() {
            this.$.ajax.url = this.url
        },
        ajaxResponse: function() { },
        ajaxError: function(e) {
            console.log(this.tagName + " - ajax-response - newURL: " + this.$.ajax.url + " - status: " + e.detail.xhr.status)
            this.status = e.detail.xhr.status
            this.message = e.detail.xhr.responseText
        }
    });

</script>
</polymer-element>