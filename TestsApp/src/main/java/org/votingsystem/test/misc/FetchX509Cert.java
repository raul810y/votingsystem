package org.votingsystem.test.misc;

import org.votingsystem.dto.DeviceDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FetchX509Cert {

    private static Logger log =  Logger.getLogger(FetchX509Cert.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        getServer();
        System.exit(0);
    }

    public static void getServer() throws Exception {
        Actor actor = TestUtils.fetchServer("https://192.168.1.5/TimeStampServer");
        Collection<X509Certificate> certCollection = PEMUtils.fromPEMToX509CertCollection(
                actor.getCertChainPEM().getBytes());
        for(X509Certificate cert: certCollection) {
            log.info(format("cert {0} - not valid after {1}", cert.getSubjectDN(), cert.getNotAfter()));
        }
        X509Certificate x509TimeStampServerCert = certCollection.iterator().next();
        log.info("subjectDN " + x509TimeStampServerCert.getSubjectDN().toString() + " - not valid after:" +
                x509TimeStampServerCert.getNotAfter());
        byte[] pemBytes = PEMUtils.getPEMEncoded(x509TimeStampServerCert);
        log.info("PEM cert: " + new String(pemBytes));
        if(new Date().after(x509TimeStampServerCert.getNotAfter())) {
            log.log(Level.SEVERE, format("{0} signing cert is lapsed - cert not valid after: {1}",
                    actor.getServerInfoURL(), x509TimeStampServerCert.getNotAfter()));
            throw new ExceptionVS(actor.getServerInfoURL() + " signing cert is lapsed");
        }
    }

    public static void getDeviceDto() throws Exception {
        String serverURL = "https://192.168.1.5/CurrencyServer/rest/device/id/2";
        ResponseVS responseVS = HttpHelper.getInstance().getData(serverURL, ContentType.JSON);
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw responseVS.getException("serverInfoURL - error");
        DeviceDto dto = (DeviceDto) responseVS.getMessage(DeviceDto.class);
        log.info(PEMUtils.fromPEMToX509Cert(dto.getX509CertificatePEM().getBytes()).toString());
    }
}

