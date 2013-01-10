package org.sistemavotacion.smime;

import java.io.OutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import iaik.pkcs.pkcs11.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class DNIeContentSigner implements ContentSigner {
    
    private static Logger logger = LoggerFactory.getLogger(DNIeContentSigner.class);    
    
    private final String signatureAlgorithm;
    private SignatureOutputStream stream = new SignatureOutputStream();

   private Session pkcs11Session = null;
            
    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
    }

    @Override
    public OutputStream getOutputStream() {
        return stream;
    }

    @Override
    public byte[] getSignature() {
        return stream.getSignature();
    }
    
    public DNIeContentSigner (String signatureAlgorithm) throws Exception {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /**
     * @return the pkcs11Session
     */
    public Session getPkcs11Session() {
        return pkcs11Session;
    }

    /**
     * @param pkcs11Session the pkcs11Session to set
     */
    public void setPkcs11Session(Session pkcs11Session) {
        this.pkcs11Session = pkcs11Session;
    }

     
    private class SignatureOutputStream extends OutputStream {
        
        //private Signature sig;
        ByteArrayOutputStream bOut;

        //SignatureOutputStream(Signature sig) {
        SignatureOutputStream() {
            bOut = new ByteArrayOutputStream();
        }

        public void write(byte[] bytes, int off, int len) throws IOException {
            bOut.write(bytes, off, len);
        }

        public void write(byte[] bytes) throws IOException {
            bOut.write(bytes);
        }

        public void write(int b) throws IOException {
            bOut.write(b);
        }
        
        
        byte[] getSignature2() {
            byte[] sigBytes = null;
            
            byte[] encoding = bOut.toByteArray();
            if (encoding[0] != (DERTags.CONSTRUCTED | DERTags.SEQUENCE)) {
                logger.debug(" -------not a digest info object");
            }
            
            
            ASN1InputStream aIn = new ASN1InputStream(encoding);
            DigestInfo digestInfo = null;
            try {
               digestInfo  = new DigestInfo((ASN1Sequence)aIn.readObject());
               sigBytes = pkcs11Session.sign(digestInfo.getEncoded(ASN1Encodable.DER));
                String sigBytesStr = new String(Base64.encode(sigBytes));
                logger.debug(" ------- sigBytesStr: " + sigBytesStr);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
            return sigBytes;
        }
        
        byte[] getSignature() {
            byte[] sigBytes = null;
            try {
               byte[] hashValue = bOut.toByteArray();
               //String hashValueStr = new String(Base64.encode(hashValue));
               //logger.debug(" ------- hashValueStr: " + hashValueStr);               
                sigBytes = pkcs11Session.sign(bOut.toByteArray());
                //String sigBytesStr = new String(Base64.encode(sigBytes));
                //logger.debug(" ------- sigBytesStr: " + sigBytesStr);
                DNIeSessionHelper.closeSession();
            } catch (Exception ex) {       
                logger.error(ex.getMessage(), ex);
            }
            return sigBytes;
        }

        byte[] getSignature1() {
            byte[] sigBytes = null;
            try {
                //DERObjectIdentifier hashAlgoId = NISTObjectIdentifiers.id_sha512;
                DERObjectIdentifier oid = OIWObjectIdentifiers.idSHA1;
                byte[] hashValue = bOut.toByteArray();
                
                AlgorithmIdentifier algId = new AlgorithmIdentifier(oid, DERNull.INSTANCE);
                DigestInfo dInfo = new DigestInfo(algId, hashValue);

                String hashValueStr = new String(Base64.encode(hashValue));
                logger.debug(" ------- DigestInfo hashValueStr: " + hashValueStr);
                //sigBytes = pkcs11Session.sign(bOut.toByteArray());
                sigBytes = pkcs11Session.sign(dInfo.getEncoded(ASN1Encodable.DER));
                String sigBytesStr = new String(Base64.encode(sigBytes));
                logger.debug(" ------- sigBytesStr: " + sigBytesStr);
                DNIeSessionHelper.closeSession();
            } catch (Exception ex) {       
                logger.error(ex.getMessage(), ex);
            }
            return sigBytes;
        }
    }
    
}



