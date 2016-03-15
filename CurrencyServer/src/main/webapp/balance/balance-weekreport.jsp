<%@ page contentType="text/html; charset=UTF-8" %>

<script src="../resources/js/balanceVSUtils_js.vsp" type="text/javascript"></script>

<dom-module name="balance-user-details">
    <template>
        <style>
            .movemenType{
                font-size: 1.2em; font-weight: bold; text-decoration: underline;color: #6c0404;
            }
            .tagLbl{ font-weight: bold; }
            .user{font-weight: bold; text-align: center; color: #888;}
            .errorMessage {color: #ba0011; border: 1px solid #ba0011; padding: 4px;}
            .card {
                position: relative;
                display: inline-block;
                width: 500px;
                vertical-align: top;
                background-color: #fff;
                box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
                margin: 10px;
                padding: 10px;
                border: 1px solid #ccc;
            }
        </style>
        <div class="card" layout vertical>
            <div hidden="{{!errorMessage}}" class="errorMessage">{{errorMessage}}</div>
            <div class="user"><span>{{infoName}}</span> - <span>{{name}}</span></div>
            <div class="layout horizontal center center-justified">
                ${msg.numExpensesLbl}: <span>{{balance.transactionFromList.length}}</span> ---
                ${msg.numIncomesLbl}: <span>{{balance.transactionToList.length}}</span>
            </div>

            <div hidden="{{!transactionToInfoMap}}">
                <template is="dom-repeat" items="{{getCurrencyList(transactionToInfoMap)}}" as="currency">
                    <div class="movemenType">${msg.incomesLbl} - <span>{{currency.currencyName}}</span></div>
                    <template is="dom-repeat" items="{{getTagVSList(currency)}}" as="tagVS">
                        <div class="layout horizontal">
                            <div class="tagLbl">{{tagVS.name}}</div>  - ${msg.numTransactionLbl}: <span>{{tagVS.numTransaction}}</span> -
                        ${msg.amountLbl}: <span>{{tagVS.amount}}</span> <span>{{currency.currencyName}}</span>
                        </div>
                    </template>
                </template>
            </div>

            <div hidden="{{!transactionFromInfoMap}}">
                <template is="dom-repeat" items="{{getCurrencyList(transactionFromInfoMap)}}" as="currency">
                    <div class="movemenType">${msg.expensesLbl} - {{currency.currencyName}}</div>
                    <template is="dom-repeat" items="{{getTagVSList(currency)}}" as="tagVS">
                        <div class="layout horizontal">
                            <div class="tagLbl">{{tagVS.name}}</div>  - ${msg.numTransactionLbl}: <span>{{tagVS.numTransaction}}</span> -
                            ${msg.amountLbl}: <span>{{tagVS.amount}}</span> <span>{{currency.currencyName}}</span>
                        </div>
                    </template>
                </template>
            </div>
        </div>

    </template>
    <script>
        Polymer({
            is:'balance-user-details',
            properties:{
                url:{type:String, observer:'getHTTP'},
                balance:{type:Object},
                type:{type:String}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                //calculateBalanceResultMap()
            },
            getCurrencyList:function(transactionFromInfoMap) {
                console.log("getCurrencyList")
                var currencyList = []
                Object.keys(transactionFromInfoMap).forEach(function(entry) {
                    var currencyDataMap = {currencyName:entry, tagVSMap:transactionFromInfoMap[entry]}
                    currencyList.push(currencyDataMap)
                });
                return currencyList
            },
            getTagVSList:function(currencyInfoMap) {
                var tagVSMap = currencyInfoMap.tagVSMap
                var tagVSList = []
                Object.keys(tagVSMap).forEach(function(entry) {
                    //{"HIDROGENO":{"numTransaction":6,"amount":"61236.00"}
                    var dataMap = {name:entry, numTransaction:tagVSMap[entry].numTransaction,
                        amount:tagVSMap[entry].amount}
                    tagVSList.push(dataMap)
                });
                return tagVSList
            },
            balanceChanged:function() {
                if(!this.balance) {
                    console.log("balanceChanged - balance null")
                    return
                }
                if(this.type === "user") {
                    this.infoName = this.balance.user.nif
                    this.name = this.balance.user.firstName + " " + this.balance.user.lastName
                }
                if(this.type === "group") {
                    this.infoName = "${msg.groupLbl}"
                    this.name = this.balance.user.name
                }
                if(this.type === "systemVS") {
                    this.infoName = "${msg.systemLbl}"
                    this.name = this.balance.user.name
                }
                this.transactionFromInfoMap = getCurrencyMap(this.balance.transactionFromList)
                this.transactionToInfoMap = getCurrencyMap(this.balance.transactionToList)

                //calculatedBalanceMap, serverBalanceMap
                try {
                    checkBalanceMap(this.transactionFromInfoMap, this.balance.balancesFrom)
                } catch(e) {
                    this.errorMessage = "transactionFrom - " + e
                }
                try {
                    checkBalanceMap(this.transactionToInfoMap, this.balance.balancesTo)
                } catch(e) {
                    this.errorMessage = "transactionTo - " + e
                }
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.balance = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>

<dom-module name="balance-weekreport">
    <template>
        <!--JavaFX Webkit gives problems with tables and templates -->
        <div class="layout center center-justified" style="margin: 0px auto 0px auto;">
            <div class="layout flex horizontal wrap around-justified">
                <balance-user-details type="systemVS" balance="{{balances.userBalances.systemBalance}}"></balance-user-details>
            </div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{balances.userBalances.groupBalanceList}}" as="groupBalance">
                    <balance-user-details type="group" balance="{{groupBalance}}"></balance-user-details>
                </template>
            </div>
            <div class="layout flex horizontal wrap around-justified">
                <template is="dom-repeat" items="{{balances.userBalances.userBalanceList}}" as="userBalance">
                    <balance-user-details  type="user" balance="{{userBalance}}"></balance-user-details>
                </template>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'balance-weekreport',
            properties: {
                balances: {type:Object}
            },
            ready: function() {},
            stringify:function(e) {
                return JSON.stringify(e)
            },
            balancesChanged: function() {
                console.log("balancesChanged")
                this.balancesStr = JSON.stringify(this.balances)
            },
            formatAmount: function(amount) {
                if(typeof amount == 'number') return amount.toFixed(2)
            }
        });
    </script>
</dom-module>