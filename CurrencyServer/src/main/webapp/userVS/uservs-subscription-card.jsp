<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module name="uservs-subscription-card">
    <template>
        <style>
        .userCard {
            width: 250px;
            background-color: #f9f9f9;
            box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.24);
            -moz-border-radius: 3px; border-radius: 4px;
            border: 1px solid rgba(0, 0, 0, 0.24);
            margin: 5px;
            color: #667;
            cursor: pointer;
            padding: 3px;
        }
        .nifColumn {text-decoration:underline; font-size: 0.9em;}
        .date {margin:0 10px 0 0; color: #0000ff; font-size: 0.7em;}
        .userVSname {color: #888; margin: 3px 3px 5px 3px; white-space: nowrap; overflow: hidden;
            text-overflow: ellipsis; font-size: 0.8em;}
        .stateInfo {color:#621; text-transform: uppercase; text-align: right; font-size: 0.8em;
            font-weight: bold;text-align: left;}
        </style>
        <div class="userCard" on-click="userSelected">
            <div hidden="{{nifHidden}}" class="nifColumn">{{subscription.uservs.nif}}</div>
            <!--<div style="width:200px;">{{subscription.uservs.iban}}</div>-->
            <div class="horizontal layout center-justified userVSname flex">{{subscription.uservs.name}}</div>
            <div class="horizontal layout center center-justified">
                <div class="flex" class="stateInfo">{{userState(subscription.state)}}</div>
                <div class="date">{{getDate(subscription.dateCreated)}}</div>
            </div>
        </div>
    </template>
    <script>
        Polymer({
            is:'uservs-subscription-card',
            properties: {
                subscription: {type:Object, value: {}},
                nifHidden: {type:Boolean, value: false}
            },
            getDate:function(dateStamp) {
                return new Date(dateStamp).getDayWeekFormat()
            },
            ready: function() { console.log(this.tagName + " - ready") },
            userState: function(state) {
                var userState
                switch(state) {
                    case 'ACTIVE':
                        userState = '${msg.activeUserLbl}'
                        break;
                    case 'PENDING':
                        userState = '${msg.pendingUserLbl}'
                        break;
                    case 'CANCELED':
                        userState = '${msg.cancelledUserLbl}'
                        break;
                    default:
                        userState = jsonSubscriptionData.state
                }
                return userState
            },
            userSelected: function(e) {
                console.log(this.tagName + " - userSelected - userId: " + this.subscription.uservs.id)
                this.fire('iron-signal', {name: "uservs-selected", data: this.subscription.uservs.id});
            }
        });
    </script>
</dom-module>
