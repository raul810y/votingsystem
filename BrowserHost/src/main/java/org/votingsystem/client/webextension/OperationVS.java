package org.votingsystem.client.webextension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.event.EventHandler;
import org.votingsystem.client.webextension.dialog.*;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.service.InboxService;
import org.votingsystem.client.webextension.service.WebSocketAuthenticatedService;
import org.votingsystem.client.webextension.task.*;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.MessageDto;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.util.*;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationVS implements PasswordDialog.Listener {

    private static Logger log = Logger.getLogger(OperationVS.class.getName());

    private TypeVS operation;
    private Integer statusCode;
    private String message;
    private String nif;
    private String documentURL;
    private String serverURL;
    private String serviceURL;
    private String receiverName;
    private String email;
    private String tabId;
    private File file;
    private String signedMessageSubject;
    private String jsonStr;
    private String contentType;
    private EventVSDto eventVS;
    private VoteVSDto voteVS;
    private UserVSDto userVS;
    private String UUID;
    private char[] password;
    private String callerCallback;
    private EventHandler closeListener;
    @JsonIgnore private ActorVS targetServer;

    private ExecutorService executorService;

    public OperationVS() {}

    public String getServerURL() {
        return serverURL;
    }

    public TypeVS getType() {
        return getOperation();
    }

    public String getCaption() {
        return ContextVS.getInstance().getMessage(operation.toString());
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentURL() {
        return documentURL;
    }

    public void setDocumentURL(String documentURL) {
        this.documentURL = documentURL;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    public EventVSDto getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVSDto eventVS) {
        this.eventVS = eventVS;
    }

    public Map getDocumentToSign() {
        Map documentToSignMap = null;
        try {
            documentToSignMap = JSON.getMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            documentToSignMap.put("UUID", java.util.UUID.randomUUID().toString());
        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return documentToSignMap;
    }

    public <T> T getDocumentToSign(Class<T> type) throws Exception {
        return JSON.getMapper().readValue(jsonStr, type);
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getReceiverName() {
        if(receiverName != null) return receiverName;
        if(targetServer != null) return targetServer.getName();
        return null;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getSignedMessageSubject() {
        return signedMessageSubject;
    }

    public void setSignedMessageSubject(String signedMessageSubject) {
        this.signedMessageSubject = signedMessageSubject;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCallerCallback() {
        return callerCallback;
    }

    public void setCallerCallback(String callerCallback) {
        this.callerCallback = callerCallback;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ActorVS getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(ActorVS targetServer) {
        this.targetServer = targetServer;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public OperationVS setOperation(TypeVS operation) {
        this.operation = operation;
        return this;
    }

    public <T> T getData(Class<T> type) throws IOException {
        return JSON.getMapper().readValue(jsonStr, type);
    }

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    public VoteVSDto getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVSDto voteVS) {
        this.voteVS = voteVS;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getTabId() {
        return tabId;
    }

    public void setTabId(String tabId) {
        this.tabId = tabId;
    }
    
    public void processOperationWithPassword(final String passwordDialogMessage) {
        if(CryptoTokenVS.MOBILE != BrowserSessionService.getCryptoTokenType()) {
            PlatformImpl.runAndWait(() ->
                    PasswordDialog.showWithoutPasswordConfirm(this, passwordDialogMessage));
        } else processPassword(null);
    }

    private String getOperationMessage() {
        String msgSuffix = "";
        String msgPrefix = "";
        if(CryptoTokenVS.MOBILE == BrowserSessionService.getCryptoTokenType()) msgSuffix = " - " + ContextVS.getMessage("messageToDeviceProgressMsg",
                BrowserSessionService.getInstance().getCryptoToken().getDeviceName());
        switch (operation) {
            case CURRENCY_REQUEST:
                try {
                    TransactionVSDto requestDto = JSON.getMapper().readValue(jsonStr.getBytes(), TransactionVSDto.class);
                    msgPrefix = signedMessageSubject + "\n\n" + ContextVS.getMessage("currencyRequestMsg",
                            requestDto.getAmount(), requestDto.getCurrencyCode(),
                            MsgUtils.getTagDescription(requestDto.getTagName()));
                } catch (IOException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                break;
            default:
                msgPrefix = signedMessageSubject;
        }
        return msgPrefix + msgSuffix;
    }

    public void saveWallet() {
        PasswordDialog.showWithoutPasswordConfirm(this, ContextVS.getMessage("walletPinMsg"));
    }

    @Override
    public void processPassword(char[] password) {
        this.password = password;
        if(password == null && CryptoTokenVS.MOBILE != BrowserSessionService.getCryptoTokenType()) {
            processResult(new ResponseVS(ResponseVS.SC_CANCELED, ContextVS.getMessage("operationCancelledMsg")));
            BrowserHost.sendMessageToBrowser(MessageDto.DIALOG_CLOSE(tabId));
        } else {
            try {
                switch (operation) {
                    case WALLET_SAVE:
                        if(BrowserHost.getInstance().loadWallet(password) != null) {
                            BrowserHost.getInstance().loadWallet(password);
                            processResult(ResponseVS.OK());
                            InboxService.getInstance().removeMessagesByType(TypeVS.CURRENCY_IMPORT);
                        }
                        break;
                    case PUBLISH_EVENT:
                        ProgressDialog.show(new SendSMIMETask(this, jsonStr, getOperationMessage(), password, "eventURL"), null);
                        break;
                    case SEND_VOTE:
                        ProgressDialog.show(new VoteTask(this, getOperationMessage(), password), null);
                        break;
                    case CANCEL_VOTE:
                        ProgressDialog.show(new CancelVoteTask(this, getOperationMessage(), password), null);
                        break;
                    case CERT_USER_NEW:
                        ProgressDialog.show(new CertRequestTask(this, getOperationMessage(), password), null);
                        break;
                    case CURRENCY_REQUEST:
                        ProgressDialog.show(new CurrencyRequestTask(this, getOperationMessage(), password), null);
                        break;
                    case REPRESENTATIVE_SELECTION:
                        RepresentativeDelegationDto delegationDto = getDocumentToSign(RepresentativeDelegationDto.class);
                        delegationDto.setUUID(java.util.UUID.randomUUID().toString());
                        ProgressDialog.show(new SendSMIMETask(this, JSON.getMapper().writeValueAsString(delegationDto),
                                getOperationMessage(), password), null);
                        break;
                    case EDIT_REPRESENTATIVE:
                        Map documentMap = getDocumentToSign();
                        //remove the 'image/jpeg;base64' part from the string
                        String representativeImage = ((String)documentMap.get("base64Image")).split(",")[1];
                        documentMap.put("base64Image", representativeImage);
                        ProgressDialog.show(new SendSMIMETask(this, JSON.getMapper().writeValueAsString(documentMap),
                                getOperationMessage(), password), null);
                        break;
                    case ANONYMOUS_REPRESENTATIVE_SELECTION:
                        ProgressDialog.show(new AnonymousDelegationCancelTask(this, getOperationMessage(), password), null);
                        break;
                    case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELATION:
                        ProgressDialog.show(new AnonymousDelegationTask(this, getOperationMessage(), password), null);
                        break;
                    default:
                        ProgressDialog.show(new SendSMIMETask(this, jsonStr, getOperationMessage(), password), null);
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public EventHandler getCloseListener() {
        if(closeListener == null) closeListener = event -> {
            BrowserHost.sendMessageToBrowser(MessageDto.DIALOG_CLOSE(tabId));
        };
        return closeListener;
    }

    public void initProcess() throws Exception {
        log.info("initProcess - operation: " + operation);
        executorService = Executors.newSingleThreadExecutor();
        Future future = executorService.submit(() -> {
            ResponseVS responseVS;
            switch (operation) {
                case SEND_VOTE:
                    responseVS = ContextVS.getInstance().checkServer(serverURL.trim());
                    responseVS = ContextVS.getInstance().checkServer(ContextVS.getInstance().getAccessControl().getControlCenter().getServerURL());
                    break;
                case CONNECT:
                    if(ContextVS.getInstance().getCurrencyServer() == null) {
                        if(serverURL == null) serverURL = ContextVS.getMessage("defaultCurrencyServer");
                        responseVS = ContextVS.getInstance().checkServer(serverURL.trim());
                    } else responseVS = new ResponseVS(ResponseVS.SC_OK);
                    break;
                case INIT_SERVER:
                    responseVS = ContextVS.getInstance().checkServer(serverURL.trim());
                    break;
                default:
                    responseVS = ContextVS.getInstance().checkServer(serverURL.trim());
            }
            return responseVS;
        });
        ResponseVS responseVS = (ResponseVS) future.get();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            processResult(responseVS);
            return;
        } else if(responseVS.getData() != null && responseVS.getData() instanceof ActorVS) setTargetServer((ActorVS) responseVS.getData());
        switch (operation) {
            case TOOL_VS://we only want to show the main dialog
                BrowserHost.getInstance().toFront();
                executorService.shutdown();
                break;
            case INIT_SERVER:
                break;
            case CONNECT:
                WebSocketAuthenticatedService.getInstance().setConnectionEnabled(true);
                break;
            case DISCONNECT:
                WebSocketAuthenticatedService.getInstance().setConnectionEnabled(false);
                break;
            case FILE_FROM_URL:
                executorService.submit(() -> {
                    ResponseVS response = HttpHelper.getInstance().getData(documentURL, null);
                    if(ResponseVS.SC_OK == response.getStatusCode()) {
                        try {
                            DocumentBrowserDialog.showDialog(response.getMessageBytes(), event -> {
                                BrowserHost.sendMessageToBrowser(MessageDto.DIALOG_CLOSE(tabId));
                            });
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    }
                });
                break;
            case OPEN_SMIME:
                try {
                    DocumentBrowserDialog.showDialog(Base64.getDecoder().decode(message.getBytes()), event -> {
                        BrowserHost.sendMessageToBrowser(MessageDto.DIALOG_CLOSE(tabId));
                    });
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
                break;
            case CURRENCY_OPEN:
                CurrencyDialog.show((Currency) ObjectUtils.deSerializeObject((message).getBytes()),
                        BrowserHost.getInstance().getScene().getWindow(), getCloseListener());
                break;
            case OPEN_SMIME_FROM_URL:
                executorService.submit(() -> {
                    try {
                        ResponseVS response = null;
                        if(BrowserHost.getInstance().getSMIME(serviceURL) != null) {
                            response = new ResponseVS(ResponseVS.SC_OK, BrowserHost.getInstance().getSMIME(serviceURL));
                        } else {
                            response = HttpHelper.getInstance().getData(serviceURL, ContentTypeVS.TEXT);
                            if (ResponseVS.SC_OK == response.getStatusCode()) {
                                BrowserHost.getInstance().setSMIME(serviceURL, response.getMessage());
                            }
                        }
                        if (ResponseVS.SC_OK == response.getStatusCode()) {
                            response.setStatusCode(ResponseVS.SC_INITIALIZED);
                            try {
                                DocumentBrowserDialog.showDialog(response.getMessageBytes(), event -> {
                                    BrowserHost.sendMessageToBrowser(MessageDto.DIALOG_CLOSE(tabId));
                                });
                            } catch (Exception ex) {
                                log.log(Level.SEVERE, ex.getMessage(), ex);
                            }
                        }
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                        BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                    }
                });
                break;
            case SEND_ANONYMOUS_DELEGATION:
                Utils.saveReceiptAnonymousDelegation(this);
                break;
            case CERT_USER_NEW:
                processOperationWithPassword(ContextVS.getMessage("newCertPasswDialogMsg"));
                break;
            case WALLET_OPEN:
                WalletDialog.showDialog();
                break;
            case WALLET_SAVE:
                saveWallet();
                break;
            case MESSAGEVS:
                processResult(WebSocketAuthenticatedService.getInstance().sendMessageVS(this));
                break;
            case REPRESENTATIVE_STATE:
                RepresentationStateDto dto = BrowserSessionService.getInstance().getRepresentationState();
                responseVS = new ResponseVS(ResponseVS.SC_OK, JSON.getMapper().writeValueAsString(dto)).setContentType(ContentTypeVS.JSON);
                processResult(responseVS);
                break;
            case CURRENCY_REQUEST:
                processOperationWithPassword(getOperationMessage());
                break;
            default:
                processOperationWithPassword(signedMessageSubject);
        }
    }

    public void processResult(ResponseVS responseVS) {
        log.log(Level.INFO, "processResult - statusCode: " + responseVS.getStatusCode());
        try {
            switch (operation) {
                case PUBLISH_EVENT:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        String eventURL = ((List<String>)responseVS.getData()).iterator().next() +"?menu=admin";
                        log.info("publishSMIME - new event URL: " + eventURL);
                        BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(eventURL));
                        //String receipt = responseVS.getMessage();
                        responseVS.setMessage(ContextVS.getMessage("eventVSPublishedOKMsg"));
                    }
                    BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                    break;
                case EDIT_REPRESENTATIVE:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        UserVSDto representativeDto = (UserVSDto) responseVS.getMessage(UserVSDto.class);
                        String representativeURL =  ContextVS.getInstance().getAccessControl()
                                .getRepresentativeByNifServiceURL(representativeDto.getNIF());
                        BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(representativeURL));
                        responseVS.setMessage(ContextVS.getMessage("representativeDataSendOKMsg"));
                    }
                    BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                    break;
                case CURRENCY_GROUP_EDIT:
                case CURRENCY_GROUP_NEW:
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        GroupVSDto dto = (GroupVSDto) responseVS.getMessage(GroupVSDto.class);
                        String groupVSURL =  ContextVS.getInstance().getCurrencyServer().getGroupURL(dto.getId());
                        BrowserHost.sendMessageToBrowser(MessageDto.NEW_TAB(groupVSURL));
                        responseVS.setMessage(ContextVS.getMessage("groupVSDataSendOKMsg"));
                    }
                    BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                    break;
                default:
                    if(callerCallback == null) BrowserHost.showMessage(responseVS.getStatusCode(), responseVS.getMessage());
                    else BrowserHost.sendMessageToBrowser(MessageDto.OPERATION_CALLBACK(
                                responseVS.getStatusCode(), responseVS.getMessage(), tabId, callerCallback));


            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            BrowserHost.showMessage(ResponseVS.SC_ERROR, ex.getMessage());
        } finally {
            if(executorService != null) executorService.shutdown();
            BrowserHost.sendMessageToBrowser(MessageDto.DIALOG_CLOSE(tabId));
        }
    }

    public UserVSDto getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }
}
