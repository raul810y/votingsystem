package org.votingsystem.model.currency;

import org.votingsystem.model.UserVS;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("BankVS")
public class BankVS extends UserVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public BankVS() {
        setType(Type.BANKVS);
    }

    public static BankVS getUserVS (X509Certificate certificate) {
        BankVS userVS = new BankVS();
        userVS.setCertificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.contains("C=")) userVS.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER=")) userVS.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME=")) userVS.setLastName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME=")) {
            String givenname = subjectDN.split("GIVENNAME=")[1].split(",")[0];
            userVS.setName(givenname);
            userVS.setFirstName(givenname);
        }
        if (subjectDN.contains("CN=")) userVS.setCn(subjectDN.split("CN=")[1]);
        return userVS;
    }
}
