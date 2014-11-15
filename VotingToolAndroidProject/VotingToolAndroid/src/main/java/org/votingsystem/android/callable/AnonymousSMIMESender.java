package org.votingsystem.android.callable;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import javax.mail.Header;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonymousSMIMESender implements Callable<ResponseVS> {

    public static final String TAG = AnonymousSMIMESender.class.getSimpleName();
;
    private AppContextVS contextVS;
    private CertificationRequestVS certificationRequest;
    private String fromUser;
    private String toUser;
    private String textToSign;
    private String subject;
    private String serviceURL;
    private X509Certificate receiverCert;
    private ContentTypeVS contentType;
    private Header header;

    public AnonymousSMIMESender(String fromUser, String toUser, String textToSign, String subject,
            Header header, String serviceURL, X509Certificate receiverCert,
            ContentTypeVS contentType,CertificationRequestVS certificationRequest,
            AppContextVS context) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.textToSign = textToSign;
        this.subject = subject;
        this.header = header;
        this.serviceURL = serviceURL;
        this.receiverCert = receiverCert;
        this.contextVS = context;
        this.contentType = contentType;
        this.certificationRequest = certificationRequest;
    }

    @Override public ResponseVS call() {
        LOGD(TAG + ".call", "");
        ResponseVS responseVS = null;
        try {
            SMIMEMessage signedMessage = certificationRequest.getSMIME(fromUser, toUser,
                    textToSign, subject, header);
            MessageTimeStamper timeStamper = new MessageTimeStamper(signedMessage, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
                return responseVS;
            }
            signedMessage = timeStamper.getSMIME();
            byte[] messageToSend = Encryptor.encryptSMIME(signedMessage, receiverCert);
            responseVS = HttpHelper.sendData(messageToSend, contentType, serviceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage receipt = Encryptor.decryptSMIME(responseVS.getMessageBytes(),
                        certificationRequest.getKeyPair().getPrivate());
                responseVS.setSMIME(receipt);
            } else return responseVS;
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, contextVS);
        } finally { return responseVS; }
    }

}