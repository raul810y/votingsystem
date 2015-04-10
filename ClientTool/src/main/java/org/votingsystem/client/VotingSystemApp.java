package org.votingsystem.client;

import javafx.application.Application;
import javafx.stage.Stage;
import org.votingsystem.client.service.SessionService;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebSocketSession;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.AESParams;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ContextVS;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VotingSystemApp extends Application {

    private static Logger log = Logger.getLogger(VotingSystemApp.class.getSimpleName());

    private static VotingSystemApp INSTANCE;
    private Map<String, String> smimeMessageMap;
    private static final Map<String, WebSocketSession> sessionMap = new HashMap<String, WebSocketSession>();
    private Long deviceId;

    // Create a trust manager that does not validate certificate chains
    private static TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                System.out.print("trustAllCerts - getAcceptedIssuers");
                try {
                    return ContextVS.getInstance().getVotingSystemSSLCerts().toArray(new X509Certificate[]{});
                } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex);}
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                System.out.print("trustAllCerts - checkClientTrusted");
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType ) throws CertificateException {
                System.out.print("trustAllCerts - checkServerTrusted");
                try {
                    CertUtils.verifyCertificate(ContextVS.getInstance().getVotingSystemSSLTrustAnchors(), false,
                            Arrays.asList(certs));
                } catch(Exception ex) {
                    throw new CertificateException(ex.getMessage());
                }
            }
        }
    };

    public String getSMIME(String smimeMessageURL) {
        if(smimeMessageMap ==  null) return null;
        else return smimeMessageMap.get(smimeMessageURL);
    }

    public void setSMIME(String smimeMessageURL, String smimeMessageStr) {
        if(smimeMessageMap ==  null) {
            smimeMessageMap = new HashMap<String, String>();
        }
        smimeMessageMap.put(smimeMessageURL, smimeMessageStr);
    }

    public void putWSSession(String UUID, WebSocketSession session) {
        sessionMap.put(UUID, session.setUUID(UUID));
    }

    public AESParams getWSSessionKeys(String UUID) {
        WebSocketSession webSocketSession = null;
        if((webSocketSession = sessionMap.get(UUID)) != null) return webSocketSession.getAESParams();
        return null;
    }

    public WebSocketSession getWSSession(String UUID) {
        return sessionMap.get(UUID);
    }
    public WebSocketSession getWSSession(Long deviceId) {
        List<WebSocketSession> result = sessionMap.entrySet().stream().filter(k ->  k.getValue().getDeviceVS() != null &&
                k.getValue().getDeviceVS().getId() == deviceId).map(k -> k.getValue()).collect(toList());
        return result.isEmpty()? null : result.get(0);
    }

    @Override public void stop() {
        log.info("stop");
        System.exit(0);//Platform.exit();
    }

    public static VotingSystemApp getInstance() {
        return INSTANCE;
    }

    @Override public void start(final Stage primaryStage) throws Exception {
        INSTANCE = this;
        BrowserVS browserVS = BrowserVS.init(primaryStage);
        new Thread(() -> {
                boolean loadedFromJar = false;
                if(VotingSystemApp.class.getResource(VotingSystemApp.this.getClass().getSimpleName() + ".class").
                        toString().contains("jar:file")) {
                    loadedFromJar = true;
                }
                log.info("start - loadedFromJar: " + loadedFromJar + " - JavaFX version: " +
                        com.sun.javafx.runtime.VersionInfo.getRuntimeVersion());
                try {
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                } catch (GeneralSecurityException ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
                String accessControlServerURL = null;
                String currencyServerURL = null;
                if(loadedFromJar) {
                    accessControlServerURL = ContextVS.getMessage("prodAccessControlServerURL");
                    currencyServerURL = ContextVS.getMessage("prodCurrencyServerURL");
                } else {
                    accessControlServerURL = ContextVS.getMessage("devAccessControlServerURL");
                    currencyServerURL = ContextVS.getMessage("devCurrencyServerURL");
                }
                ResponseVS responseVS = null;
                try {
                    CookieManager cookieManager = new CookieManager();
                    Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
                    headers.put("Set-Cookie", Arrays.asList("Accept-Language=" + Locale.getDefault().getLanguage()));
                    cookieManager.put(new URI(accessControlServerURL), headers);
                    cookieManager.put(new URI(currencyServerURL), headers);
                    CookieHandler.setDefault(cookieManager);
                    responseVS = Utils.checkServer(accessControlServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        browserVS.setVotingSystemAvailable(true);
                        ContextVS.getInstance().setAccessControl((AccessControlVS) responseVS.getData());
                        SessionService.getInstance().checkCSRRequest();
                    }
                } catch(Exception ex) {log.log(Level.SEVERE,ex.getMessage(), ex);}
                try {
                    responseVS = Utils.checkServer(currencyServerURL);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        browserVS.setCurrencyServerAvailable(true);
                        ContextVS.getInstance().setCurrencyServer((CurrencyServer) responseVS.getData());
                    } else browserVS.setCurrencyServerAvailable(false);
                } catch(Exception ex) {
                    log.log(Level.SEVERE,ex.getMessage());
                    browserVS.setCurrencyServerAvailable(false);
                }
        }).start();
        browserVS.show();
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public static void main(String[] args) {
        ContextVS.initSignatureClient("clientToolMessages", Locale.getDefault().getLanguage());
        if(args.length > 0) ContextVS.getInstance().initDirs(args[0]);
        launch(args);
    }

}