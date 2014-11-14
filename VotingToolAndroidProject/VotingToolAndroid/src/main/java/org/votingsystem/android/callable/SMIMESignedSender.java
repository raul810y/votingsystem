package org.votingsystem.android.callable;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.ANDROID_PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNATURE_ALGORITHM;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignedSender implements Callable<ResponseVS> {

    public static final String TAG = SMIMESignedSender.class.getSimpleName();

    private SMIMEMessage smimeMessage = null;
    private X509Certificate receiverCert = null;
    private AppContextVS contextVS = null;
    private String serviceURL = null;
    private String fromUser = null;
    private String toUser = null;
    private String subject = null;
    private String textToSign = null;
    private ContentTypeVS contentType;

    public SMIMESignedSender(String fromUser, String toUser, String serviceURL,
            String textToSign, ContentTypeVS contentType, String subject,
            X509Certificate receiverCert, AppContextVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.subject = subject;
        this.contentType = contentType;
        this.contextVS = context;
        this.serviceURL = serviceURL;
        this.receiverCert = receiverCert;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "serviceURL: " + serviceURL);
        ResponseVS responseVS = null;
        try {
            KeyStore.PrivateKeyEntry keyEntry = contextVS.getUserPrivateKey();
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(keyEntry.getPrivateKey(),
                    keyEntry.getCertificateChain(), SIGNATURE_ALGORITHM, ANDROID_PROVIDER);
            smimeMessage = signedMailGenerator.getSMIME(fromUser, toUser,textToSign, subject);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                responseVS.setCaption(contextVS.getString(R.string.timestamp_service_error_caption));
                responseVS.setNotificationMessage(responseVS.getMessage());
                return responseVS;
            }
            smimeMessage = timeStamper.getSMIME();
            byte[] messageToSend = null;
            if(contentType.isEncrypted())
                messageToSend = Encryptor.encryptSMIME(smimeMessage, receiverCert);
            else messageToSend = smimeMessage.getBytes();
            responseVS  = HttpHelper.sendData(messageToSend, contentType, serviceURL);
            if(responseVS.getContentType() != null && responseVS.getContentType().isEncrypted()) {
                if(responseVS.getContentType().isSigned()) {
                    SMIMEMessage signedMessage = Encryptor.decryptSMIME(responseVS.getMessageBytes(),
                            keyEntry.getPrivateKey());
                    responseVS.setSMIME(signedMessage);
                } else {
                    byte[] decryptedMessageBytes = Encryptor.decryptCMS(keyEntry.getPrivateKey(),
                            responseVS.getMessageBytes());
                    responseVS = new ResponseVS(ResponseVS.SC_OK, decryptedMessageBytes);
                }
            }
        } catch(ExceptionVS ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                    contextVS.getString(R.string.pin_error_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getLocalizedMessage());
        } finally {return responseVS;}
    }

    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

}