var Operation = {
    BANKVS:"BANKVS",
    BANKVS_NEW:"BANKVS_NEW",
    CERT_CA_NEW:"CERT_CA_NEW",
    CERT_USER_NEW:"CERT_USER_NEW",
    CERT_EDIT:"CERT_EDIT",
    CONNECT:"CONNECT",
    DISCONNECT:"DISCONNECT",
    FILE_FROM_URL:"FILE_FROM_URL",
    KEYSTORE_SELECT:"KEYSTORE_SELECT",
    SIGNAL_VS:"SIGNAL_VS",
    BROWSER_URL: "BROWSER_URL",
    LISTEN_TRANSACTIONS: "LISTEN_TRANSACTIONS",
    MESSAGEVS:"MESSAGEVS",
    OPEN_SMIME: "OPEN_SMIME",
    OPEN_SMIME_FROM_URL: "OPEN_SMIME_FROM_URL",
    SAVE_SMIME: "SAVE_SMIME",
    CURRENCY_OPEN: "CURRENCY_OPEN",
    FROM_GROUP_TO_MEMBER_GROUP:"FROM_GROUP_TO_MEMBER_GROUP",
    FROM_GROUP_TO_ALL_MEMBERS:"FROM_GROUP_TO_ALL_MEMBERS",
    FROM_USERVS:"FROM_USERVS",
    CURRENCY_GROUP_NEW: "CURRENCY_GROUP_NEW",
    CURRENCY_GROUP_EDIT: "CURRENCY_GROUP_EDIT",
    CURRENCY_GROUP_CANCEL: "CURRENCY_GROUP_CANCEL",
    CURRENCY_GROUP_USER_ACTIVATE: "CURRENCY_GROUP_USER_ACTIVATE",
    CURRENCY_GROUP_USER_DEACTIVATE: "CURRENCY_GROUP_USER_DEACTIVATE",
    CURRENCY_GROUP_USER_DEPOSIT: "CURRENCY_GROUP_USER_DEPOSIT",
    CURRENCY_GROUP_SUBSCRIBE: "CURRENCY_GROUP_SUBSCRIBE",
    CURRENCY_REQUEST: "CURRENCY_REQUEST",
    WALLET_OPEN: "WALLET_OPEN",
    WALLET_SAVE: "WALLET_SAVE"
}

function DateUtils(){}

//parse dates with format "2010-08-30 01:02:03" or "2010/08/30 01:02:03"
DateUtils.parse = function (dateStr) {
    var reggie = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/;
    try { (+dateArray[1]) } catch(ex) { reggie = /(\d{4})\/(\d{2})\/(\d{2}) (\d{2}):(\d{2}):(\d{2})/; }

    var dateArray = reggie.exec(dateStr);
    var dateObject = new Date(
        (+dateArray[1]),
        (+dateArray[2])-1, //Months are zero based
        (+dateArray[3]),
        (+dateArray[4]),
        (+dateArray[5]),
        (+dateArray[6])
    );
    return dateObject
}

//removes year from date with format -> 'dd MMM yyyy HH:mm'
DateUtils.trimYear = function (dateStr) {
    var reggie = /(\d{2}) (\w{3}) (\d{4}) (\d{2}):(\d{2})/;
    var dateArray = reggie.exec(dateStr);
    return dateArray[1] + " " + dateArray[2] + " " + dateArray[4] + ":" + dateArray[5]
}

//parse dates with format "dd/mm/aa"
DateUtils.parseInput = function (dateStr) {
    var reggie = /(\d{2})\/(\d{2})\/(\d{2})/;
    var dateArray = reggie.exec(dateStr);
    var dateObject = new Date(
        (+dateArray[3]) + 2000,
        (+dateArray[2])-1, //Months are zero based
        (+dateArray[1])
    );
    return dateObject
}

DateUtils.checkDate = function (dateBegin, dateFinish) {
    var todayDate = new Date();
    if(todayDate > dateBegin && todayDate < dateFinish) return true;
    else return false;
}

Date.prototype.formatWithTime = function() {
	var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date + " " + ('0' + this.getHours()).slice(-2)  + ":" +
            ('0' + this.getMinutes()).slice(-2) + ":" + ('0' + this.getSeconds()).slice(-2)
};

Date.prototype.urlFormatWithTime = function() {//YYYYMMDD_HHmm
    var curr_date = pad(this.getDate(), 2);
    var curr_month = pad(this.getMonth() + 1, 2); //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + curr_month + curr_date + "_" + ('0' + this.getHours()).slice(-2) + ('0' + this.getMinutes()).slice(-2)
};

Date.prototype.format = function() {
	var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date
};

Date.prototype.daydiff = function (dateToCompare) {
    return (this - dateToCompare)/(1000*60*60*24);
}

//parse dates with format "yyyy-mm-dd"
DateUtils.parseInputType = function (dateStr) {
		var reggie = /(\d{4})-(\d{2})-(\d{2})/;
		var dateArray = reggie.exec(dateStr);
		var dateObject = new Date(
		    (+dateArray[1]),
		    (+dateArray[2])-1, //Months are zero based
		    (+dateArray[3])
		);
		return dateObject
	}

function showMessageVS(message, caption, callerId, isConfirmMessage) {
    if (document.querySelector("#_votingsystemMessageDialog") != null && typeof
            document.querySelector("#_votingsystemMessageDialog").setMessage != 'undefined'){
        document.querySelector("#_votingsystemMessageDialog").setMessage(message, caption, callerId, isConfirmMessage)
    }  else {
        console.log('alert-dialog not found');
        window._originalAlert(message);
    }
}

String.prototype.format = function() {
	  var args = arguments;
	  var str =  this.replace(/''/g, "'")
	  return str.replace(/{(\d+)}/g, function(match, number) {
	    return typeof args[number] != 'undefined'
	      ? args[number]
	      : match
	    ;
	  });
	};

	
String.prototype.getDate = function() {
	  var timeMillis = Date.parse(this)
	  return new Date(timeMillis)
};


function pad(n, width, z) {
    z = z || '0';
    n = n + '';
    return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

String.prototype.getElapsedTime = function() {
	  return this.getDate().getElapsedTime()
};


function toJSON(message){
	if(message != null) {
		if( Object.prototype.toString.call(message) == '[object String]' ) {
			return JSON.parse(message);
		} else {
			return message
		} 
	}
}

var ResponseVS = {
		SC_OK : 200,
        SC_OK_WITHOUT_BODY:204,
		SC_ERROR_REQUEST : 400,
		SC_ERROR_REQUEST_REPEATED : 409,
        SC_PRECONDITION_FAILED:412,
        SC_MESSAGE_FROM_VS:277,
		SC_ERROR : 500,
		SC_PROCESSING : 700,
		SC_CANCELED : 0,
		SC_INITIALIZED : 1,
		SC_PAUSED:10
}

function calculateNIFLetter(dni) {
    var  nifLetters = "TRWAGMYFPDXBNJZSQVHLCKET";
    var module= dni % 23;
    return nifLetters.charAt(module);
}

function validateNIF(nif) {
	if(nif == null) return false;
	nif  = nif.toUpperCase();
	if(nif.length < 9) {
        var numZeros = 9 - nif.length;
		for(var i = 0; i < numZeros ; i++) {
			nif = "0" + nif;
		}
	}
	var number = nif.substring(0, 8);
    var letter = nif.substring(8, 9);
    if(letter != calculateNIFLetter(number)) return null;
    else return nif;
}

//http://www.mkyong.com/javascript/how-to-detect-ie-version-using-javascript/
function getInternetExplorerVersion() {
// Returns the version of Windows Internet Explorer or a -1
// (indicating the use of another browser).
   var rv = -1; // Return value assumes failure.
   if (navigator.appName == 'Microsoft Internet Explorer')
   {
      var ua = navigator.userAgent;
      var re  = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
      if (re.exec(ua) != null)
         rv = parseFloat( RegExp.$1 );
   }
   return rv;
}


function addNumbers(num1, num2) {
    return (new Number(num1) + new Number(num2)).toFixed(2)
}

function openWindow(targetURL) {
    var width = 1000
    var height = 800
    var left = (screen.width/2) - (width/2);
    var top = (screen.height/2) - (height/2);
    var title = ''

    var newWindow =  window.open(targetURL, title, 'toolbar=no, scrollbars=yes, resizable=yes, '  +
            'width='+ width +
            ', height='+ height  +', top='+ top +', left='+ left + '');
}

function isChrome () {
	return (navigator.userAgent.toLowerCase().indexOf("chrome") > - 1);
}

function isAndroid () {
	return (navigator.userAgent.toLowerCase().indexOf("android") > - 1);
}

function isFirefox () {
	return (navigator.userAgent.toLowerCase().indexOf("firefox") > - 1);
}

function isJavaFX () {
	return (navigator.userAgent.toLowerCase().indexOf("javafx") > - 1);
}

var menuType = getURLParam('menu').toLowerCase();
if(menuType == null) menuType = 'user';

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function getURLParam(name, url) {
    if(!url) url = location.search
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),  results = regex.exec(url);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function getQueryParam(variable, querystring) {
    var vars = querystring.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
    console.log('Query variable %s not found', variable);
}

function setURLParameter(baseURL, name, value){
    var result;
    if(getURLParam(name, baseURL)){
        result = baseURL.replace(new RegExp('([?|&]'+name + '=)' + '(.+?)(&|$)'),"$1"+encodeURIComponent(value)+"$3");
    }else if(baseURL.length){
        if(baseURL.indexOf("?") < 0) baseURL = baseURL + "?"
        result = baseURL +'&'+name + '=' +encodeURIComponent(value);
    } else {
        result = '?'+name + '=' +encodeURIComponent(value);
    }
    return result
}

function VotingSystemClient () { }

var clientTool
VotingSystemClient.setMessage = function (messageJSON) {
    if(window['isClientToolConnected'] || window.parent['isClientToolConnected']) {
        if(clientTool == undefined) clientTool = window.top.clientTool //we're inside vs-iframe
        var messageToSignatureClient = JSON.stringify(messageJSON);
        //https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64.btoa#Unicode_Strings
        clientTool.setMessage(window.btoa(encodeURIComponent( escape(messageToSignatureClient))))
    } else {
        console.log("clientTool undefined - operation: " + messageJSON.operation)
        if(isAndroid ()) {
            var encodedData = window.btoa(JSON.stringify(messageJSON));
            window.sendAndroidURIMessage(encodedData)
            return
        }
    }
}

function querySelector(selector) {
    if(document.querySelector(selector) != null) return document.querySelector(selector)
    else return document.querySelector('html /deep/ ' + selector)
}

function sendSignalVS(signalData) {
    var result
    var operationVS = new OperationVS(Operation.SIGNAL_VS)
    operationVS.jsonStr = JSON.stringify(signalData)
    VotingSystemClient.setMessage(operationVS);
}

window['isClientToolConnected'] = false

function checkIfClientToolIsConnected() {
    return window['isClientToolConnected'] || window.parent['isClientToolConnected']
}

function setClientToolConnected() {
    window['isClientToolConnected'] = true;
    document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('votingsystem-client-connected', { }))
}

var coreSignalData = null
function fireCoreSignal(coreSignalDataBase64) {
    window['isClientToolConnected'] = true
    if(document.querySelector("#app") != null && document.querySelector("#app").fire != null) {
        var b64_to_utf8 = decodeURIComponent(escape(window.atob(coreSignalDataBase64)))
        document.querySelector("#app").fire('iron-signal', toJSON(b64_to_utf8));
        console.log("fireCoreSignal: " + b64_to_utf8)
    } else {
        coreSignalData = coreSignalDataBase64
        console.log("fireCoreSignal - navBar not found")
    }
}

document.addEventListener('WebComponentsReady', function() {
    console.log("utilsVS.js - WebComponentsReady - sending pending core signal - coreSignalData: " + coreSignalData)
    if(coreSignalData != null) fireCoreSignal(coreSignalData)
    coreSignalData = null
});

//Message -> base64 encoded JSON
//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Base64_encoding_and_decoding#Solution_.232_.E2.80.93_rewriting_atob()_and_btoa()_using_TypedArrays_and_UTF-8
function setClientToolMessage(callerId, message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message)))
    window[callerId](b64_to_utf8)
}
