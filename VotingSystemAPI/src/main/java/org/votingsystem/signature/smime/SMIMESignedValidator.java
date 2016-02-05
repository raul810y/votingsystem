package org.votingsystem.signature.smime;

import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.util.Store;
import org.votingsystem.util.ContextVS;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMESignedValidator { 
    
    private static Logger log = Logger.getLogger(SMIMESignedValidator.class.getName());
    
    /**
     * verify that the sig is correct and that it was generated when the 
     * certificate was current(assuming the cert is contained in the message).
     */
    public static boolean isValidSignature(SMIMESigned smimeSigned) throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        log.info("signers.size(): " + signers.size());
        Collection c = signers.getSigners();
        Iterator it = c.iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            log.info("Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER)
                    .getCertificate((X509CertificateHolder)certIt.next());
            log.info("SubjectDN: " + cert.getSubjectDN() +
                    " - Not before: " + cert.getNotBefore() + " - Not after: " + cert.getNotAfter()
                    + " - SigningTime: " + getSigningTime(signer));
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(ContextVS.PROVIDER).build(cert))){
                log.info("signature verified");
                result = true;
            } else {
                log.info("signature failed!");
                result = false;
            }
        }
        return result;
    }
    
    
    public static Date getSigningTime(SignerInformation signerInformation) {
        AttributeTable signedAttr = signerInformation.getSignedAttributes(); 
        Attribute signingTime = signedAttr.get(CMSAttributes.signingTime); 
        if (signingTime != null) { 
        	try {
                Enumeration en = signingTime.getAttrValues().getObjects(); 
                while (en.hasMoreElements()) { 
                        Object obj = en.nextElement(); 
                        if (obj instanceof ASN1UTCTime) { 
                                ASN1UTCTime asn1Time = (ASN1UTCTime) obj; 
                                return asn1Time.getDate();
                        } else if (obj instanceof DERUTCTime) { 
                                DERUTCTime derTime = (DERUTCTime) obj; 
                                return derTime.getDate();
                        } 
                } 
        	} catch(Exception ex) {
        		log.log(Level.SEVERE, ex.getMessage(), ex);
        	}
        }
		return null; 
    }
	    
}
