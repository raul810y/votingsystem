<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transactionvs-list-balance">
    <template>
        <style>
        .dateCreated {font-size: 0.8em; color:#888; width: 140px; margin: 0 7px 0 0;}
        .subjectColumn {
            width:330px; overflow: hidden; text-overflow: ellipsis; white-space:nowrap; margin:0px 10px 0px 0px; font-size: 0.8em;
        }
        .amountColumn {width:130px;text-align: right; font-size: 0.9em;}
        .tagColumn {font-size: 0.6em; text-align: center; vertical-align: middle; width: 150px; text-overflow: ellipsis; padding: 0 5px 0 5px;}
        </style>
        <div class="layout vertical justified" style="display: block; height: 100%;">
            <div class="horizontal layout center center-justified" style="margin: 0 0 10px 0; min-width: 400px;">
                <div class="flex"></div>
                <div style="font-size: 0.9em;color:#888; font-weight: bold; margin:5px 20px 0 10px;
                    text-decoration: underline;"><span>{{numMovements}}</span> - <span>{{caption}}</span></div>
            </div>
            <div>
                <template is="dom-repeat" items="{{transactionList}}" as="transaction">
                    <div class="layout horizontal" on-click="viewTransaction" style="cursor: pointer;">
                        <div class="dateCreated">{{getDate(transaction.dateCreated)}}</div>
                        <div title="{{transaction.subject}}" class="subjectColumn"
                             style="margin:0 30px 0 0;">{{transaction.subject}}</div>
                        <div class="amountColumn"><span>{{transaction.amount}}</span> <span>{{transaction.currencyCode}}</span></div>
                        <div class="layout horizontal center center-justified tagColumn">
                            <div class="flex horizontal layout center center-justified">{{transaction.tags[0]}}</div>
                            <div hidden="{{!isTimeLimited(transaction)}}" style="margin:0 0 0 5px; width: 10px;">
                                <div title="${msg.timeLimitedAdviceMsg}">
                                    <i class="fa fa-clock-o"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
                <div id="rowTotal" style="display: none;">
                    <div class="layout horizontal">
                        <div class="dateCreated"></div>
                        <div class="subjectColumn" style="text-align: right;font-weight: bold; width:350px;">
                            ${msg.totalLbl}:
                        </div>
                        <div class="amountColumn" style="border-top: 1px solid #888;">{{transactionTotal}}</div>
                        <div class="tagColumn"></div>
                    </div>
                </div>
            </div>
            <div class="flex"></div>
        </div>
    </template>
    <script>
        Polymer({
            is:'transactionvs-list-balance',
            properties: {
                balances: {type:Object, value: {}, observer:'balancesChanged'},
                transactionList: {type:Array, observer:'transactionListChanged'},
                numMovements: {type:String, value: 0 + " ${msg.movementsLbl}"},
                caption: {type:String, value: "${msg.expensesLbl}"}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            isTimeLimited: function(tranctionvs) {
                return (tranctionvs.validTo != null)
            },
            viewTransaction: function(e) {
                this.fire("transactionviewer", e.model.transaction)
            },
            transactionListChanged:function() {
                this.numMovements = this.transactionList.length + " ${msg.movementsLbl}"
            },
            balancesChanged:function() {
                this.transactionTotal = 0
                if(this.balances != null && this.balances["EUR"]) {
                    Object.keys(this.balances["EUR"]).forEach(function(entry) {
                        if(this.balances["EUR"][entry].total != null) {
                            this.transactionTotal = addNumbers(this.transactionTotal, this.balances["EUR"][entry].total)
                        } else this.transactionTotal = addNumbers(this.transactionTotal, this.balances["EUR"][entry])
                    }.bind(this))
                    this.transactionTotal = new Number(this.transactionTotal).toFixed(2) + " EUR"
                    this.$.rowTotal.style.display = 'block'
                }
            }
        });
    </script>
</dom-module>