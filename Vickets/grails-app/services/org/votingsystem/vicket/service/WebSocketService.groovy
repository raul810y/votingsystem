package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONSerializer
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.model.MessageVS
import org.votingsystem.vicket.websocket.SessionVSHelper
import net.sf.json.JSONObject;
import javax.websocket.CloseReason
import javax.websocket.Session
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class WebSocketService {

    private static final ConcurrentHashMap<String, Session> connectionsMap = new ConcurrentHashMap<String, Session>();
    def grailsApplication
    def messageSource

    public void onTextMessage(Session session, String msg , boolean last) {
        JSONObject messageJSON = (JSONObject)JSONSerializer.toJSON(msg);
        messageJSON.sessionId = session.getId()
        log.debug("onTextMessage --- session id: ${session.getId()} - operaionn : ${messageJSON?.operation} - last: ${last}")
        processRequest(messageJSON, session)
    }

    public void onBinaryMessage(Session session, ByteBuffer bb, boolean last) {
        log.debug("onBinaryMessage")
        //session.getBasicRemote().sendBinary(bb, last);
    }

    public void onOpen(Session session) {
        SessionVSHelper.getInstance().put(session)
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug("onClose - session id: ${session.getId()} - closeReason: ${closeReason}")
        SessionVSHelper.getInstance().remove(session)
    }


    public void broadcast(JSONObject messageJSON) {
        SessionVSHelper.getInstance().broadcast(messageJSON)
    }

    public ResponseVS broadcastList(Map dataMap, Set<String> listeners) {
        return SessionVSHelper.getInstance().broadcastList(dataMap, listeners)
    }

    /*
    * references to services from grailsApplication to avoid circular references
    */
    public void processRequest(JSONObject messageJSON, Session session) {
        String message = null;
        Locale locale = null
        try {
            if(!messageJSON.locale) {
                messageJSON.status = ResponseVS.SC_ERROR
                messageJSON.message = "missing message locale"
                processResponse(messageJSON)
                return;
            }
            locale = Locale.forLanguageTag(messageJSON.locale)
            TypeVS socketOperation
            try { socketOperation = TypeVS.valueOf(messageJSON.operation) }
            catch(Exception ex) {
                messageJSON.status = ResponseVS.SC_ERROR
                messageJSON.message = "Invalid operation '${messageJSON.operation}'"
                processResponse(messageJSON)
                return;
            }
            switch(socketOperation) {
                case TypeVS.LISTEN_TRANSACTIONS:
                    TransactionVSService transactionVSService = grailsApplication.mainContext.getBean("transactionVSService")
                    transactionVSService.addTransactionListener(messageJSON.userId)
                    break;
                case TypeVS.MESSAGEVS:
                    MessageVSService messageVSService = grailsApplication.mainContext.getBean("messageVSService")
                    messageVSService.sendWebSocketMessage(messageJSON)
                case TypeVS.INIT_VALIDATED_SESSION:
                    SignatureVSService signatureVSService = grailsApplication.mainContext.getBean("signatureVSService")
                    SMIMEMessageWrapper smimeMessageReq = new SMIMEMessageWrapper(new ByteArrayInputStream(
                            messageJSON.smimeMessage.decodeBase64()))
                    messageJSON.remove("smimeMessage")
                    ResponseVS responseVS = signatureVSService.processSMIMERequest(smimeMessageReq, null, new Locale(messageJSON.locale))
                    if(ResponseVS.SC_OK == responseVS.statusCode) {
                        SessionVSHelper.getInstance().put(session, ((MessageSMIME)responseVS.data).userVS)
                        processResponse(getInitValidatedSessionResponseMsg(((MessageSMIME)responseVS.data).userVS,
                                messageJSON))
                    } else {
                        messageJSON.status = ResponseVS.SC_ERROR
                        messageJSON.message = responseVS.getMessage()
                        processResponse(messageJSON)
                    }
                    break;
                default: throw new ExceptionVS(messageSource.getMessage("unknownSocketOperationErrorMsg",
                        [messageJSON.operation].toArray(), locale))
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            messageJSON.status = ResponseVS.SC_ERROR
            messageJSON.message = ex.getMessage()
            processResponse(messageJSON)
        }
    }

    @Transactional
    public JSONObject getInitValidatedSessionResponseMsg(UserVS userVS, JSONObject requestJSON) {
        JSONObject responseJSON = new JSONObject(requestJSON)
        responseJSON.userId = userVS.id
        def pendingMessageVSList = MessageVS.findAllWhere(toUserVS: userVS, state:MessageVS.State.PENDING)
        List messageVSList = []
        pendingMessageVSList.each { messageVS ->
            JSONObject messageVSJSON = JSONSerializer.toJSON(messageVS.content, "UTF-8");
            Map fromUser = [id:messageVS.fromUserVS.id, name:messageVS.fromUserVS.getDefaultName()]
            messageVSList.add([fromUser: fromUser, dateCreated:messageVS.dateCreated,
                   encryptedMessage:messageVSJSON.encryptedMessage, receiverCertSerialNumber:messageVS.receiverCertSerialNumber])
        }
        responseJSON.messageVSList = messageVSList
        responseJSON.status = ResponseVS.SC_OK
        return responseJSON
    }


    public void processResponse(JSONObject messageJSON) {
        SessionVSHelper.getInstance().sendMessage(messageJSON.sessionId, messageJSON.toString());
    }

}