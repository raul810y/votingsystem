document.querySelector("#voting_system_page").addEventListener('message-to-host',
    function(event) {
        chrome.runtime.sendMessage({operation:'message-to-host', content:event.detail}, function(response) {
            console.log("message-from-host");
        });
    })

document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('message_from_extension'))


chrome.extension.onMessage.addListener(function(msg, sender, sendResponse) {
    if(msg.message_type === "message_to_webextension") {
        if(msg.operation === "dialog_closed") {
            console.log("content.js ---------- dialog_closed")
        }
    } else {
        document.querySelector("#voting_system_page").dispatchEvent(new CustomEvent('message_from_extension', {detail:msg}))
    }
});


console.log("content.js - ready - chrome.runtime.id: " + chrome.runtime.id);