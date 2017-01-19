package org.votingsystem.crypto;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.util.Constants;
import org.votingsystem.util.JSON;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificationRequest implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    
    private static Logger log = Logger.getLogger(CertificationRequest.class.getName());

    private transient PKCS10CertificationRequest csr;
    private transient KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;

    private CertificationRequest(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequest getVoteRequest(String indentityServiceEntity, String votingServiceEntity,
            String electionUUID, String revocationHashBase64) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException, IOException, OperatorCreationException {
        KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
        X500Name subject = new X500Name("CN=identityService:" + indentityServiceEntity +
                ";votingService:" + votingServiceEntity + ", OU=electionUUID:" + electionUUID);
        CertVoteExtensionDto dto = new CertVoteExtensionDto(indentityServiceEntity, votingServiceEntity,
                revocationHashBase64, electionUUID);
        PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.VOTE_OID),
                new DERUTF8String(JSON.getMapper().writeValueAsString(dto)));
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.ANON_CERT_OID), ASN1Boolean.getInstance(true));
        PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                Constants.SIGNATURE_ALGORITHM).setProvider(Constants.PROVIDER).build(keyPair.getPrivate()));
        return new CertificationRequest(keyPair, request, Constants.SIGNATURE_ALGORITHM);
    }

    public static CertificationRequest getUserRequest (String signatureMechanism, CertExtensionDto certExtensionDto)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException, OperatorCreationException {
        KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal(certExtensionDto.getPrincipal());
        PKCS10CertificationRequestBuilder pkcs10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        pkcs10Builder.addAttribute(new  ASN1ObjectIdentifier(Constants.DEVICE_OID),
                new DERUTF8String(JSON.getMapper().writeValueAsString(certExtensionDto)));
        PKCS10CertificationRequest request = pkcs10Builder.build(new JcaContentSignerBuilder(
                signatureMechanism).setProvider(Constants.PROVIDER).build(keyPair.getPrivate()));
        return new CertificationRequest(keyPair, request, signatureMechanism);
    }
    
    public byte[] signDataWithTimeStamp(byte[] cotentToSign, String timeStampServiceURL) throws Exception {
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(signedCsr);
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        return org.votingsystem.xades.XAdESSignature.sign(cotentToSign,
                new SignatureTokenConnection(keyPair.getPrivate(), arrayCerts), new TSPHttpSource(timeStampServiceURL));
    }

    public X509Certificate getCertificate() throws Exception {
        if(certificate == null)
            return PEMUtils.fromPEMToX509Cert(signedCsr);
        return certificate;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() throws Exception {
        return keyPair.getPrivate();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public byte[] getCsrPEM() throws Exception {
        return PEMUtils.getPEMEncoded(csr);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(certificate != null) s.writeObject(certificate.getEncoded());
            else s.writeObject(null);
            if(keyPair != null) {//this is to deserialize private keys outside android environments
                s.writeObject(keyPair.getPublic().getEncoded());
                s.writeObject(keyPair.getPrivate().getEncoded());
            } else {
                s.writeObject(null);
                s.writeObject(null);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            InvalidKeySpecException {
        s.defaultReadObject();
        byte[] certificateBytes = (byte[]) s.readObject();
        if(certificateBytes != null) {
            try {
                certificate = CertUtils.loadCertificate(certificateBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] publicKeyBytes = (byte[]) s.readObject();
            PublicKey publicKey =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            byte[] privateKeyBytes = (byte[]) s.readObject();
            PrivateKey privateKey =  KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            if(privateKey != null && publicKey != null) keyPair = new KeyPair(publicKey, privateKey);
        } catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
    }

    public byte[] getSignedCsr() {
        return signedCsr;
    }

    public CertificationRequest setSignedCsr(byte[] signedCsr) {
        this.signedCsr = signedCsr;
        return this;
    }

}