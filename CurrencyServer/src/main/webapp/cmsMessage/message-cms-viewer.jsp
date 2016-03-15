<%@ page contentType="text/html; charset=UTF-8" %>
<dom-module name="message-cms-viewer">
    <template>
        <div id="messageViewer" class="layout vertical center center-justified" style="margin: 0px auto; max-width:800px;">
        </div>
    </template>
    <script>
        Polymer({
            is:'message-cms-viewer',
            properties: {
                cmsMessageDto:{type:Object, value:{}, observer:'cmsMessageChanged'},
                MESSAGE_CMS:{type:Object, value:null},
                CURRENCY_GROUP_NEW:{type:Object, value:null},
                FROM_BANK:{type:Object, value:null},
                CURRENCY_REQUEST:{type:Object, value:null},
                FROM_GROUP_TO_ALL_MEMBERS:{type:Object, value:null},
                CURRENCY_CHANGE:{type:Object, value:null},
                viewer: {type:String, value: ""},
                url:{type:String, observer:'getHTTP'},
                isClientToolConnected: {type:Boolean, value: false}
            },
            cmsMessageChanged: function() {
                switch (this.cmsMessageDto.viewer) {
                    case "message-cms":
                        break;
                    case "message-cms-transactionvs-from-bank":
                        if(!this.FROM_BANK) {
                            Polymer.Base.importHref(vs.contextURL + '/cmsMessagePEM/message-cms-transactionvs-from-bank.vsp', function(e) {
                                console.log(this.tagName + " - message-cms-transactionvs-from-bank: " + this.FROM_BANK)
                                this.FROM_BANK = document.createElement('message-cms-transactionvs-from-bank');
                                this.loadMainContent(this.FROM_BANK)
                            }.bind(this));
                        } else this.loadMainContent(this.FROM_BANK)
                        break;
                    case "message-cms-transactionvs-currency-request":
                        break;
                    case "message-cms-transactionvs":
                        break;
                    case "message-cms-transactionvs-currency-change":
                        break;
                }
            },
            loadMainContent: function(element) {
                element.cmsMessageDto = this.cmsMessageDto
                vs.loadMainContent(element)
            },
            ready: function() {
                console.log(this.tagName + " - ready")
                this.isClientToolConnected = (clientTool !== undefined) || vs.webextension_available
            },
            getHTTP: function (targetURL) {
                if(!targetURL) targetURL = this.url
                console.log(this.tagName + " - getHTTP - targetURL: " + targetURL)
                d3.xhr(targetURL).header("Content-Type", "application/json").get(function(err, rawData){
                    this.cmsMessageDto = toJSON(rawData.response)
                }.bind(this));
            }
        });
    </script>
</dom-module>