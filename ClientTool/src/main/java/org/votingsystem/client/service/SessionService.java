package org.votingsystem.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.application.PlatformImpl;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import javafx.scene.control.Button;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.model.Representation;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.dto.AnonymousDelegationDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.BrowserVS.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionService {

    private static Logger log = Logger.getLogger(SessionService.class.getSimpleName());

    private UserVS userVS;
    private File sessionFile;
    private File representativeStateFile;
    private AnonymousDelegationDto anonymousDelegationDto;
    private Map representativeStateMap;
    private Map browserSessionDataMap;
    private Map sessionDataMap;
    private static CountDownLatch countDownLatch;
    private static SMIMEMessage smimeMessage;
    private static ResponseVS<SMIMEMessage> messageToDeviceResponse;
    private static final SessionService INSTANCE = new SessionService();

    private SessionService() {
        try {
            sessionFile = new File(ContextVS.APPDIR + File.separator + ContextVS.BROWSER_SESSION_FILE);
            if(sessionFile.createNewFile()) {
                sessionDataMap = new HashMap<>();
                browserSessionDataMap = new HashMap<>();
                browserSessionDataMap.put("deviceId", UUID.randomUUID().toString());
                browserSessionDataMap.put("fileType", ContextVS.BROWSER_SESSION_FILE);
                sessionDataMap.put("browserSession", browserSessionDataMap);
            } else {
                sessionDataMap = new ObjectMapper().readValue(sessionFile, new TypeReference<HashMap<String, Object>>() {});
                browserSessionDataMap = (Map) sessionDataMap.get("browserSession");
            }
            browserSessionDataMap.put("isConnected", false);
            if(browserSessionDataMap.get("userVS") != null) userVS =
                    UserVS.parse((Map) browserSessionDataMap.get("userVS"));
            flush();
        } catch (Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    private void loadRepresentationData() throws IOException {
        Map userVSStateOnServerMap = null;
        if(userVS != null) {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ContextVS.getInstance().getAccessControl().
                    getRepresentationStateServiceURL(userVS.getNif()), ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                userVSStateOnServerMap = responseVS.getMessageMap();
            }
        }
        representativeStateFile = new File(ContextVS.APPDIR + File.separator + ContextVS.REPRESENTATIVE_STATE_FILE);
        if(representativeStateFile.createNewFile()) {
            representativeStateMap = userVSStateOnServerMap;
            flush();
        } else {
            representativeStateMap = new ObjectMapper().readValue(
                    representativeStateFile, new TypeReference<HashMap<String, Object>>() {});
            if(userVSStateOnServerMap != null) {
                if(!userVSStateOnServerMap.get("base64ContentDigest").equals(
                        representativeStateMap.get("base64ContentDigest"))) {
                    representativeStateMap = userVSStateOnServerMap;
                    flush();
                }
            }
        }
    }

    public void setAnonymousDelegationDto(AnonymousDelegationDto delegation) {
        try {
            loadRepresentationData();
            String serializedDelegation = new String(ObjectUtils.serializeObject(delegation), "UTF-8");
            representativeStateMap.put("state", Representation.State.WITH_ANONYMOUS_REPRESENTATION.toString());
            representativeStateMap.put("lastCheckedDate", DateUtils.getDateStr(new Date()));
            representativeStateMap.put("representative", delegation.getRepresentative().toMap());
            representativeStateMap.put("anonymousDelegationObject", serializedDelegation);
            representativeStateMap.put("dateFrom", DateUtils.getDayWeekDateStr(delegation.getDateFrom()));
            representativeStateMap.put("dateTo", DateUtils.getDayWeekDateStr(delegation.getDateTo()));
            representativeStateMap.put("base64ContentDigest", delegation.getCancelVoteReceipt().getContentDigestStr());
            this.anonymousDelegationDto = delegation;
            flush();
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    public Map getRepresentationState() {
        Map result = null;
        try {
            loadRepresentationData();
            result = new HashMap<>(representativeStateMap);
            result.remove("anonymousDelegationObject");
            Representation.State representationState =
                    Representation.State.valueOf((String) representativeStateMap.get("state"));
            String stateMsg = null;
            switch (representationState) {
                case WITH_ANONYMOUS_REPRESENTATION:
                    stateMsg = ContextVS.getMessage("withAnonymousRepresentationMsg");
                    break;
                case REPRESENTATIVE:
                    stateMsg = ContextVS.getMessage("userRepresentativeMsg");
                    break;
                case WITH_PUBLIC_REPRESENTATION:
                    stateMsg = ContextVS.getMessage("withPublicRepresentationMsg");
                    break;
                case WITHOUT_REPRESENTATION:
                    stateMsg = ContextVS.getMessage("withoutRepresentationMsg");
                    break;
            }
            Date lastCheckedDate = DateUtils.getDateFromString((String) representativeStateMap.get("lastCheckedDate"));
            result.put("stateMsg", stateMsg);
            result.put("lastCheckedDateMsg", ContextVS.getMessage("lastCheckedDateMsg",
                    DateUtils.getDayWeekDateStr(lastCheckedDate)));
        } catch(Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex);
        } finally {
            return result;
        }
    }

    public AnonymousDelegationDto getAnonymousDelegationDto() {
        if(anonymousDelegationDto != null) return anonymousDelegationDto;
        try {
            loadRepresentationData();
            String serializedDelegation = (String) representativeStateMap.get("anonymousDelegationObject");
            if(serializedDelegation != null) {
                anonymousDelegationDto = (AnonymousDelegationDto) ObjectUtils.deSerializeObject(
                        serializedDelegation.getBytes());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        } finally {
            return anonymousDelegationDto;
        }
    }

    public static SessionService getInstance() {
        return INSTANCE;
    }

    public void setIsConnected(boolean isConnected) {
        browserSessionDataMap.put("isConnected", isConnected);
        flush();
    }

    public WebSocketMessage initAuthenticatedSession(WebSocketMessage socketMsg, UserVS userVS) {
        try {
            if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                socketMsg.getMessageJSON().put("userVS", userVS.toMap());
                socketMsg.setUserVS(userVS);
                browserSessionDataMap.put("userVS", userVS.toMap());
                browserSessionDataMap.put("isConnected", true);
                flush();
                VotingSystemApp.getInstance().setDeviceId(socketMsg.getDeviceId());
                BrowserVS.getInstance().runJSCommand(
                        socketMsg.getWebSocketCoreSignalJSCommand(WebSocketMessage.ConnectionStatus.OPEN));
            } else {
                showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                log.log(Level.SEVERE,"ERROR - initAuthenticatedSession - statusCode: " + socketMsg.getStatusCode());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
        return socketMsg;
    }

    public void setCSRRequestId(Long id) {
        browserSessionDataMap.put("csrRequestId", id);
        flush();
    }

    public void setCryptoToken(CryptoTokenVS cryptoTokenVS, Map deviceDataMap) {
        log.info("setCryptoToken - type: " + cryptoTokenVS.toString() + "- deviceDataJSON: " + deviceDataMap);
        ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN, cryptoTokenVS.toString());
        if(deviceDataMap == null) deviceDataMap = new HashMap<>();
        deviceDataMap.put("type", cryptoTokenVS.toString());
        browserSessionDataMap.put("cryptoTokenVS", deviceDataMap);
        flush();
    }

    //{"id":,"deviceName":"","certPEM":""}
    public Map getCryptoToken() {
        return (Map) browserSessionDataMap.get("cryptoTokenVS");
    }

    public static CryptoTokenVS getCryptoTokenType () {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        return CryptoTokenVS.valueOf(tokenType);
    }

    public String getCryptoTokenName() {
        if(browserSessionDataMap.containsKey("cryptoTokenVS")) {
            if(((Map)browserSessionDataMap.get("cryptoTokenVS")).containsKey("deviceName")) {
                return (String)((Map)browserSessionDataMap.get("cryptoTokenVS")).get("deviceName");
            } else return null;
        } else return null;
    }

    public Long getCSRRequestId() {
        return ((Number)browserSessionDataMap.get("csrRequestId")).longValue();
    }

    public String getDeviceId() {
        return (String) browserSessionDataMap.get("deviceId");
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS, boolean isConnected) throws Exception {
        this.userVS = userVS;
        List userVSList = null;
        if(browserSessionDataMap.containsKey("userVSList")) {
            userVSList = (List) browserSessionDataMap.get("userVSList");
            boolean updated = false;
            for(int i = 0; i < userVSList.size(); i++) {
                Map user = (Map) userVSList.get(i);
                if(user.get("nif").equals(userVS.getNif())) {
                    userVSList.remove(i);
                    userVSList.add(userVS.toMap());
                    updated = true;
                }
            }
            if(!updated) userVSList.add(userVS.toMap());
        } else {
            userVSList = new ArrayList<>();
            userVSList.add(userVS.toMap());
            browserSessionDataMap.put("userVSList", userVSList);
        }
        browserSessionDataMap.put("isConnected", isConnected);
        browserSessionDataMap.put("userVS", userVS.toMap());
        flush();
    }

    public Map getBrowserSessionData() {
        return browserSessionDataMap;
    }

    public void setCSRRequest(Long requestId, Encryptor.EncryptedBundle bundle) {
        try {
            File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
            csrFile.createNewFile();
            Map dataMap = bundle.toMap();
            dataMap.put("requestId", requestId);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(dataMap.toString().getBytes()), csrFile);
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }


    public void checkCSRRequest() {
        PlatformImpl.runLater(() -> checkCSR());
    }

    private void deleteCSR() {
        log.info("deleteCSR");
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) csrFile.delete();
    }

    private void checkCSR() {
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) {
            log.info("csr request found");
            try {
                Map dataMap = new ObjectMapper().readValue(csrFile, new TypeReference<HashMap<String, Object>>() {});
                String serviceURL = ContextVS.getInstance().getAccessControl().getUserCSRServiceURL(
                        ((Number)dataMap.get("requestId")).longValue());
                ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, null);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                            responseVS.getMessage().getBytes());
                    X509Certificate userCert = certificates.iterator().next();
                    UserVS user = UserVS.getUserVS(userCert);
                    setUserVS(user, false);
                    log.info("user: " + user.getNif() + " - certificates.size(): " + certificates.size());
                    X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                    certificates.toArray(certsArray);
                    String passwd = null;
                    byte[] serializedCertificationRequest = null;
                    while(passwd == null) {
                        PasswordDialog passwordDialog = new PasswordDialog();
                        passwordDialog.show(ContextVS.getMessage("csrPasswMsg"));
                        passwd = passwordDialog.getPassword();
                        if(passwd == null) {
                            Button optionButton = new Button(ContextVS.getMessage("deletePendingCsrMsg"));
                            optionButton.setGraphic(Utils.getIcon(FontAwesomeIconName.TIMES, Utils.COLOR_RED_DARK));
                            optionButton.setOnAction(event -> deleteCSR());
                            showMessage(ContextVS.getMessage("certPendingMissingPasswdMsg"), optionButton);
                            return;
                        }
                        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(dataMap);
                        try {
                            serializedCertificationRequest = Encryptor.pbeAES_Decrypt(passwd, bundle);
                        } catch (Exception ex) {
                            passwd = null;
                            showMessage(ContextVS.getMessage("cryptoTokenPasswdErrorMsg"), ContextVS.getMessage("errorLbl"));
                        }
                    }
                    CertificationRequestVS certificationRequest =
                            (CertificationRequestVS) ObjectUtils.deSerializeObject(serializedCertificationRequest);
                    KeyStore userKeyStore = KeyStore.getInstance("JKS");
                    userKeyStore.load(null);
                    userKeyStore.setKeyEntry(ContextVS.KEYSTORE_USER_CERT_ALIAS, certificationRequest.getPrivateKey(),
                            passwd.toCharArray(), certsArray);
                    ContextVS.saveUserKeyStore(userKeyStore, passwd);
                    ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                            CryptoTokenVS.JKS_KEYSTORE.toString());
                    showMessage(ResponseVS.SC_OK, ContextVS.getMessage("certInstallOKMsg"));
                    csrFile.delete();
                } else showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("certPendingMsg"));
            } catch (Exception ex) {
                log.log(Level.SEVERE,ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("errorStoringKeyStoreMsg"));
            }
        }

    }

    public Map getConnectDataMap() {
        if(getUserVS() == null) return null;
        Map<String, String> result = new HashMap<>();
        result.put("nif", getUserVS().getNif());
        return result;
    }

    public static SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
            String password, String subject, Header... headers) throws Exception {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        log.info("getSMIME - tokenType: " + tokenType);
        switch(CryptoTokenVS.valueOf(tokenType)) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getInstance().getUserKeyStore(password.toCharArray());
                SMIMESignedGeneratorVS signedGenerator = new SMIMESignedGeneratorVS(keyStore,
                        ContextVS.KEYSTORE_USER_CERT_ALIAS, password.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
                return signedGenerator.getSMIME(fromUser, toUser, textToSign, subject, headers);
            case DNIe:
                return DNIeContentSigner.getSMIME(fromUser, toUser, textToSign, password.toCharArray(), subject, headers);
            case MOBILE:
                countDownLatch = new CountDownLatch(1);
                DeviceVS deviceVS = DeviceVS.parse(getInstance().getCryptoToken());
                Map jsonObject = WebSocketMessage.getSignRequest(deviceVS, toUser, textToSign, subject, headers);
                PlatformImpl.runLater(() -> {//Service must only be used from the FX Application Thread
                    try {
                        WebSocketService.getInstance().sendMessage(jsonObject.toString());
                    } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
                });
                countDownLatch.await();
                ResponseVS<SMIMEMessage> responseVS = getMessageToDeviceResponse();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                else return responseVS.getData();
            default: return null;
        }
    }

    public static void setSignResponse(WebSocketMessage socketMsg) {
        switch(socketMsg.getStatusCode()) {
            case ResponseVS.SC_WS_MESSAGE_SEND_OK:
                break;
            case ResponseVS.SC_WS_CONNECTION_NOT_FOUND:
                messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR,
                        ContextVS.getMessage("deviceVSTokenNotFoundErrorMsg"));
                countDownLatch.countDown();
                break;
            case ResponseVS.SC_ERROR:
                messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, socketMsg.getMessage());
                countDownLatch.countDown();
                break;
            default:
                try {
                    smimeMessage = socketMsg.getSMIME();
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_OK, null, smimeMessage);
                } catch(Exception ex) {
                    log.log(Level.SEVERE,ex.getMessage(), ex);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, ex.getMessage());
                }
                countDownLatch.countDown();
        }
    }

    public static ResponseVS getMessageToDeviceResponse() {
        return messageToDeviceResponse;
    }

    private void flush() {
        log.info("flush");
        try {
            sessionDataMap.put("browserSession", browserSessionDataMap);
            new ObjectMapper().writeValue(sessionFile, sessionDataMap);
            if(representativeStateMap != null) new ObjectMapper().writeValue(representativeStateFile, representativeStateMap);
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

}
