<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="transactionvs-selector">
    <template>
        <select id="transactionvsTypeSelect" style="margin:0px auto 10px auto;color:black; max-width: 400px;"
                class="form-control" on-change="transactionvsTypeSelect">
            <option value="ALL" style="color:black;"> - ${msg.selectTransactionTypeLbl} - </option>
            <option value="CURRENCY_REQUEST"> - ${msg.selectCurrencyRequestLbl} - </option>
            <option value="CURRENCY_SEND"> - ${msg.selectCurrencySendLbl} - </option>
            <option value="CURRENCY_CHANGE"> - ${msg.selectCurrencyChangeLbl} - </option>
            <option value="CURRENCY_PERIOD_INIT"> - ${msg.currencyPeriodInitLbl} - </option>
            <option value="CURRENCY_PERIOD_INIT_TIME_LIMITED"> - ${msg.currencyPeriodInitTimeLimitedLbl} - </option>
            <option value="FROM_BANKVS"> - ${msg.bankVSInputLbl} - </option>
            <option value="FROM_USERVS"> - ${msg.transactionVSFromUserVS} - </option>
            <option value="FROM_GROUP_TO_MEMBER_GROUP"> - ${msg.transactionVSFromGroupToMemberGroup} - </option>
            <option value="FROM_GROUP_TO_ALL_MEMBERS"> - ${msg.transactionVSFromGroupToAllMembers} - </option>
        </select>
    </template>
    <script>
        Polymer({
            is:'transactionvs-selector',
            properties: {
                transactionvsType:{type:String, observer:'transactionvsTypeChanged'}
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                if(this.transactionvsType) this.$.transactionvsTypeSelect.value = this.transactionvsType
            },
            transactionvsTypeSelect:function() {
                this.fire("selected", this.$.transactionvsTypeSelect.value)
            },
            transactionvsTypeChanged: function() {
                this.$.transactionvsTypeSelect.value = this.transactionvsType
            }
        });
    </script>
</dom-module>
