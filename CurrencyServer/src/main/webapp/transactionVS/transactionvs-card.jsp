<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transactionvs-card">
    <template>
        <style>
        .card {
            position: relative;
            display: inline-block;
            width: 350px;
            vertical-align: top;
            background-color: #f9f9f9;
            box-shadow: 0 5px 5px 0 rgba(0, 0, 0, 0.24);
            border: 1px solid rgba(0, 0, 0, 0.24);
            margin: 10px;
            color: #667;
            cursor: pointer;
        }
        .date {margin:3px 10px 0 0; color: #888; font-size: 0.8em;}
        .transactionDescription {color:#621; font-size: 1.2em; text-decoration: underline;}
        .subject {color: #888; margin: 3px 3px 5px 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 0.8em;}
        .amount {color: #f9f9f9; background:#621; font-weight: bold; padding: 1px 5px 1px 5px;font-size: 0.9em;}
        .tag {color:#388746; margin: 0 0 0 5px;font-size: 0.9em;}
        .timeInfo {color:#621; text-transform: uppercase; text-align: right; margin: 0 5px 0 0; font-size: 0.8em;}
        .expenseTrans { background: #fee; }
        </style>
        <div on-click="showTransactionDetails" class="card {{getClass(transaction)}}">
            <div class="horizontal layout">
                <div class="flex horizontal layout center-justified transactionDescription">{{transactionDescription(transaction)}}</div>
                <div class="date">{{getDate(transaction.dateCreated)}}</div>
            </div>
            <div id="subjectDiv" class="subject horizontal layout center-justified">{{getSubject(transaction)}}</div>
            <div class="horizontal layout">
                <div class="amount">{{amount(transaction)}}</div>
                <div class="tag">{{tag}}</div>
                <div class="flex" class="timeInfo">{{timeLimitedDescription(transaction)}}</div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'transactionvs-card',
            properties: {
                transaction: {type:Object, observer:'transactionChanged'}
            },
            ready: function() {
                this.isConfirmMessage = this.isConfirmMessage || false
            },
            getSubject: function(transaction) {
                this.isConfirmMessage = this.isConfirmMessage || false
                if("${msg.selectCurrencyRequestLbl}" === transaction.subject) return null
                else return transaction.subject
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            getClass: function(transactionvs) {
                if(!this.isUserVSTable) return
                if(transactionvs.fromUserVS && transactionvs.fromUserVS.nif == this.userNif) {
                    return "expenseTrans"
                } else return ""
            },
            timeLimitedDescription: function(transactionvs) {
                if(transactionvs.timeLimited === true || transactionvs.validTo != null) return "${msg.timeLimitedLbl}"
                else if  (transactionvs.timeLimited === false) return ""
                else return transactionvs.timeLimited
            },
            transactionDescription: function(transactionvs) {
                var result = null
                if(this.userNif) {
                    if(transactionvs.fromUserVS && transactionvs.fromUserVS.nif == this.userNif) {
                        result = "${msg.spendingLbl} - "
                    }
                    if(transactionvs.toUserVS && transactionvs.toUserVS.nif == this.userNif) {
                        result = result? result + "${msg.incomeLbl} - " : "${msg.incomeLbl} - "
                    }
                }
                var transactionTypeLbl = transactionsMap[transactionvs.type].lbl
                return result? result.toUpperCase() + transactionTypeLbl : transactionTypeLbl
            },
            amount: function(transaction) {
                var amount
                if(isNaN(transaction.amount)) amount = transaction.amount.toFixed(2) + " " + transaction.currency
                else  amount = transaction.amount + " " + transaction.currencyCode
                return amount
            },
            transactionChanged: function(e) {
                this.tag = this.transaction.tags[0]
            },
            showTransactionDetails: function(e) {
                console.log("cmsMessageURL: " + this.transaction.cmsMessageURL)
                page.show(this.transaction.cmsMessageURL)
            }
        });
    </script>
</dom-module>