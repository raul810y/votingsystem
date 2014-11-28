var Operation = {
    CERT_USER_NEW:"CERT_USER_NEW",
    SAVE_SMIME: "SAVE_SMIME",
    SAVE_SMIME_ANONYMOUS_DELEGATION:"SAVE_SMIME_ANONYMOUS_DELEGATION",
    OPEN_SMIME: "OPEN_SMIME",
    CONTROL_CENTER_ASSOCIATION : "CONTROL_CENTER_ASSOCIATION",
    BACKUP_REQUEST: "BACKUP_REQUEST",
    MANIFEST_PUBLISHING: "MANIFEST_PUBLISHING",
    MANIFEST_SIGN: "MANIFEST_SIGN",
    CLAIM_PUBLISHING: "CLAIM_PUBLISHING",
    SMIME_CLAIM_SIGNATURE: "SMIME_CLAIM_SIGNATURE",
    VOTING_PUBLISHING: "VOTING_PUBLISHING",
    SEND_SMIME_VOTE: "SEND_SMIME_VOTE",
    CANCEL_VOTE:"CANCEL_VOTE",
    SELECT_IMAGE:"SELECT_IMAGE",
    TERMINATED: "TERMINATED",
    ACCESS_REQUEST_CANCELLATION:"ACCESS_REQUEST_CANCELLATION",
    EVENT_CANCELLATION: "EVENT_CANCELLATION",
    SIGNAL_VS:"SIGNAL_VS",
    NEW_REPRESENTATIVE:"NEW_REPRESENTATIVE",
    REPRESENTATIVE_SELECTION:"REPRESENTATIVE_SELECTION",
    ANONYMOUS_REPRESENTATIVE_SELECTION:"ANONYMOUS_REPRESENTATIVE_SELECTION",
    REPRESENTATIVE_VOTING_HISTORY_REQUEST: "REPRESENTATIVE_VOTING_HISTORY_REQUEST",
    REPRESENTATIVE_ACCREDITATIONS_REQUEST: "REPRESENTATIVE_ACCREDITATIONS_REQUEST",
    REPRESENTATIVE_REVOKE: "REPRESENTATIVE_REVOKE",
    REPRESENTATIVE_DATA:"REPRESENTATIVE_DATA",
    ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:"ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED",
    REPRESENTATIVE_STATE:"REPRESENTATIVE_STATE"
}

function httpGet(theUrl){
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false );
    xmlHttp.send( null );
    return xmlHttp.responseText;
}

function DateUtils(){}

//parse dates with format "2010-08-30 01:02:03"
DateUtils.parse = function (dateStr) {
		var reggie = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/;
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

DateUtils.checkDate = function (dateInit, dateFinish) {
		var todayDate = new Date();
		if(todayDate > dateInit && todayDate < dateFinish) return true;
		else return false;
	}

Date.prototype.formatWithTime = function() {
    var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date + " " + ('0' + this.getHours()).slice(-2)  + ":" +
        ('0' + this.getMinutes()).slice(-2) + ":" + ('0' + this.getSeconds()).slice(-2)
};

Date.prototype.format = function() {
    var curr_date = this.getDate();
    var curr_month = this.getMonth() + 1; //Months are zero based
    var curr_year = this.getFullYear();
    return curr_year + "/" + curr_month + "/" + curr_date
};

function pad(n, width, z) {
    z = z || '0';
    n = n + '';
    return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function getDatePickerValue(datePickerId, htmlElement) {
    if(!htmlElement) {
        htmlElement = document
    }
    var day = pad(htmlElement.querySelector('#' + datePickerId + '_day').value, 2)
    var month = pad(htmlElement.querySelector('#' + datePickerId + '_month').value, 2)
    var year = htmlElement.querySelector('#' + datePickerId + '_year').value
    var hour = "00"
    var minute = "00"
    var second = "00"
    if(htmlElement.querySelector('#' + datePickerId + '_minute') != null) minute =
        htmlElement.querySelector('#' + datePickerId + '_minute').value
    if(htmlElement.querySelector('#' + datePickerId + '_hour') != null) hour =
        htmlElement.querySelector('#' + datePickerId + '_hour').value
    var dateStr = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second
    return DateUtils.parse(dateStr)
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

function FormUtils(){}

FormUtils.checkIfEmpty = function (param) {
    if((param == undefined) || (param == null) || '' == param.trim()) return true;
    else return false
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

function updateLinksVS(elementsArray) {
    for (var i = 0; i < elementsArray.length; i++) {
        //console.log("elementsArray[i].href: " + elementsArray[i].href)
        if(elementsArray[i].href.indexOf("${grailsApplication.config.grails.serverURL}") > -1) {
            elementsArray[i].addEventListener('click', function(e) {
                document.querySelector('#navBar').loadURL(e.target.href)
                e.preventDefault()
            });
        } else if("" != elementsArray[i].href.trim()) console.log("main.gsp - not system url: " + elementsArray[i].href)
    }
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
		SC_ERROR_REQUEST : 400,
		SC_ERROR_REQUEST_REPEATED : 409,
		SC_ERROR : 500,
		SC_PROCESSING : 700,
		SC_CANCELLED : 0,
		SC_INITIALIZED : 1,
		SC_PAUSED:10
}

var SubSystem = {
		VOTES : "VOTES",
		CLAIMS: "CLAIMS",
		MANIFESTS: "MANIFESTS",
		REPRESENTATIVES:"REPRESENTATIVES",
		FEEDS:"FEEDS"
}


function getUrlParam(paramName, staticURL, decode){
   var currLocation = (staticURL.length)? staticURL : window.location.search,
       parArr = currLocation.split("?")[1].split("&");
   
   for(var i = 0; i < parArr.length; i++){
        parr = parArr[i].split("=");
        if(parr[0] == paramName){
            return (decode) ? decodeURIComponent(parr[1]) : parr[1];
        }
   }
}

function loadjsfile(filename){
	var fileref=document.createElement('script')
	fileref.setAttribute("type","text/javascript")
 	fileref.setAttribute("src", filename)
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

function checkInputType(inputType) {
    if(navigator.userAgent.toLowerCase().indexOf("javafx") > -1) return false;
    if(null == inputType || '' == inputType.trim()) return false
    var isSuppported = true
    var elem = document.createElement("input");
    elem.type = inputType;
    if (elem.disabled || elem.type != inputType) isSuppported = false;
    if("text" != inputType.toLowerCase()) {
        try {
            elem.value = "Test";
            if(elem.value == "Test") isSuppported = false;
        } catch(e) { console.log(e) }
    }
    return isSuppported
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

function getFnName(fn) {
	  var f = typeof fn == 'function';
	  var s = f && ((fn.name && ['', fn.name]) || fn.toString().match(/function ([^\(]+)/));
	  return (!f && 'not a function') || (s && s[1] || 'anonymous');
}

function getRadioValue(radioName) {
    var radios = document.getElementsByName(radioName);
    for (var i = 0, length = radios.length; i < length; i++) {
        if (radios[i].checked) {
            return radios[i].value;
            break;
        }
    }
}

var menuType = 'user'

if(getParameterByName('menu') != null) menuType = getParameterByName('menu')

function updateMenuLinks() {
    var elem = 'a'
    var attr = 'href'
    var elems = document.getElementsByTagName(elem);
    var arrayElements = Array.prototype.slice.call(elems);
    var groupElements = document.getElementsByClassName('linkvs');
    arrayElements.concat(Array.prototype.slice.call(groupElements))
    for (var i = 0; i < elems.length; i++) {
        if(elems[i][attr].indexOf("mailto:") > -1) continue
        if(elems[i][attr].indexOf("menu=" + menuType) < 0) {
            if(elems[i][attr].indexOf("?") < 0) {
                elems[i][attr] = elems[i][attr] + "?menu=" + menuType;
            } else elems[i][attr] = elems[i][attr] + "&menu=" + menuType;
        }
    }
    for (var j = 0; j < groupElements.length; j++) {
        var attrValue = groupElements[j].getAttribute("data-href")
        if(attrValue == null) continue
        if(attrValue.indexOf("menu=" + menuType) < 0) {
            if(attrValue.indexOf("?") < 0) {
                groupElements[j].setAttribute("data-href", attrValue + "?menu=" + menuType )
            } else groupElements[j].setAttribute("data-href", attrValue + "&menu=" + menuType );
        }
    }
}

function loadURL_VS(urlToLoad, target) {
    if(target) {
        window.open(updateMenuLink(urlToLoad), target);
    } else {
        if(document.querySelector('#navBar')) document.querySelector('#navBar').loadURL(urlToLoad)
        else window.location.href = updateMenuLink(urlToLoad, "&mode=simplePage")
    }
}

function updateMenuLink(urlToUpdate, param) {
    if(urlToUpdate == null) return
    var result = urlToUpdate
    if(result.indexOf("menu=") < 0) {
        if(result.indexOf("?") < 0) result = result + "?menu=" + menuType
        else result = result + "&menu=" + menuType
    }
    if(param != null) result = result + "&" + param
    return result
}


//http://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function getParameterByName(name, url) {
    if(!url) url = location.search
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),  results = regex.exec(url);
    return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function setURLParameter(baseURL, name, value){
    var result;
    if(getParameterByName(name, baseURL)){
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

VotingSystemClient.setJSONMessageToSignatureClient = function (messageJSON) {
    try {
        console.log("setJSONMessageToSignatureClient - clientTool: " + clientTool)
    } catch(e) {
        console.log(e)
        if(isAndroid ()) {
            var encodedData = window.btoa(JSON.stringify(messageJSON));
            window.sendAndroidURIMessage(encodedData)
            return
        }
        window.alert(e)
        return
    }
    var messageToSignatureClient = JSON.stringify(messageJSON);
//    console.log("setJSONMessageToSignatureClient - message: " + messageToSignatureClient);
    //https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64.btoa#Unicode_Strings
    clientTool.setJSONMessageToSignatureClient(window.btoa(encodeURIComponent( escape(messageToSignatureClient))))
}

function sendSignalVS(signalData, callback) {
    var result
    var webAppMessage = new WebAppMessage(Operation.SIGNAL_VS)
    webAppMessage.document = signalData
    webAppMessage.setCallback(callback)
    try {
        result = VotingSystemClient.call(webAppMessage);
    } catch(ex) { } finally { return result;}
}

VotingSystemClient.call = function (messageJSON) {
    try {
        clientTool
        var messageToSignatureClient = JSON.stringify(messageJSON);
        return clientTool.call(window.btoa(encodeURIComponent( escape(messageToSignatureClient))))
    } catch(e) {
        console.log(e)
    }
}

window['isClientToolConnected'] = false

var coreSignalData = null
function fireCoreSignal(coreSignalDataBase64) {
    window['isClientToolConnected'] = true
    if(document.querySelector("#navBar") != null && document.querySelector("#navBar").fire != null) {
        var b64_to_utf8 = decodeURIComponent(escape(window.atob(coreSignalDataBase64)))
        document.querySelector("#navBar").fire('core-signal', toJSON(b64_to_utf8));
        console.log("fireCoreSignal: " + b64_to_utf8)
    } else {
        coreSignalData = coreSignalDataBase64
        console.log("fireCoreSignal - navBar not found")
    }
}

window.addEventListener('polymer-ready', function() {
    console.log("utilsVS.js - polymer-ready - sending pending core signal")
    if(coreSignalData != null) fireCoreSignal(coreSignalData)
    coreSignalData = null
});

//Message -> base64 encoded JSON
//https://developer.mozilla.org/en-US/docs/Web/JavaScript/Base64_encoding_and_decoding#Solution_.232_.E2.80.93_rewriting_atob()_and_btoa()_using_TypedArrays_and_UTF-8
function setClientToolMessage(callerId, message) {
    var b64_to_utf8 = decodeURIComponent(escape(window.atob(message)))
    window[callerId](b64_to_utf8)
}
