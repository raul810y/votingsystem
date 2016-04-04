<%@ page contentType="text/html; charset=UTF-8" %>

<link href="../resources/forgePKCS7.html" rel="import"/>

<dom-module name="bank-data">
<template>
    <style>
        .accountBlock { border: 1px solid #888; margin: 10px 15px 10px 15px;
            background-color: #f9f9f9; min-width: 100px;
            box-shadow: 0 3px 3px 0 rgba(0, 0, 0, 0.24); cursor: pointer; display: table;
        }
        .accountBalance { font-size: 1.2em; color: #434343; text-align: center;  padding: 10px; }
        .tagDesc { background: #888; color: #f9f9f9; padding: 5px;text-align: center; }
    </style>
    <div class="pagevs">
        <div class="layout horizontal center center-justified"
             style="font-size: 1.5em;margin:5px 0 15px 0;color:#6c0404;border-bottom: 1px dotted #888;">
            <div data-user-id$="{{bank.id}}" style="text-align: center;">{{bankName}}</div>
            <div style="font-size: 0.7em; color: #888; font-weight: normal;margin: 2px 0 0 30px;">{{bank.nif}}</div>
        </div>
        <div hidden="{{!bank.description}}"  class="layout horizontal center center-justified" style="margin:0px 0px 10px 0px;">
            <div id="bankDescriptionDiv" style="padding:10px;"></div>
        </div>
        <h3 class="sectionHeader" style="margin: 30px 0 20px 0;">${msg.systemInputsLbl}</h3>

        <div class="vertical layout center">
            <template is="dom-repeat" items="{{currencyList}}" as="currency">
                <div style="color:#888;border-bottom: 1px solid #888;font-weight: bold;">{{currency}}</div>
                <div class="layout horizontal center center-justified" style="border-bottom: 1px solid #ccc;width: 100%;">
                    <template is="dom-repeat" items="{{getTagList(currency)}}" as="tag">
                        <div class="accountBlock">
                            <div class="accountBalance"><span>{{tag.amount}}</span> <span style="font-size: 0.9em;">
                                {{getCurrencySymbol(tag.currencyCode)}}</span></div>
                            <div class="tagDesc">{{tag.name}}</div>
                        </div>
                    </template>
                </div>
            </template>

        </div>

    </div>
</template>
<script>
    Polymer({
        is:'bank-data',
        properties: {
            bank: {type:Object, observer:'bankChanged'},
            url:{type:String, observer:'getHTTP'}
        },
        ready: function() {
            console.log(this.tagName + " - ready - menuType: " + this.menuType)
        },
        getTagList:function(currency) {
            var result = this.bankIncomesMap[currency];
            console.log("getTagList - currency: " + currency, result, "bankIncomesMap: ", this.bankIncomesMap)
            return result
        },
        getCurrencySymbol:function(currencyCode) {
            return vs.getCurrencySymbol(currencyCode)
        },
        bankChanged:function() {
            console.log(this.tagName + " - bankChanged - bank: ", this.bank)
            this.bankName = this.bank.name
            this.$.bankDescriptionDiv.innerHTML = this.bank.description
         },
        getHTTP: function (targetURL) {
            if(!targetURL) targetURL = this.url
            console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
            vs.getHTTPJSON(targetURL, function(responseText){
                var bankIncomesData = toJSON(responseText)
                this.bank = bankIncomesData.bank
                this.bankIncomesMap = bankIncomesData.tagBalanceList
                this.currencyList = Object.keys(this.bankIncomesMap)
            }.bind(this))
        }
    });
</script>
</dom-module>
