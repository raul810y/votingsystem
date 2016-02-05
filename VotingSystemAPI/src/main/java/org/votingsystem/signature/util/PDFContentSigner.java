package org.votingsystem.signature.util;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.votingsystem.util.ContextVS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class PDFContentSigner extends CMSSignedGenerator implements ContentSignerVS {
   
    private static Logger log = Logger.getLogger(PDFContentSigner.class.getName());

    public static String CERT_STORE_TYPE = "Collection";
            
    private PrivateKey privateKey = null;
    private X509Certificate userCert = null;
    private Certificate[] signerCertChain = null;
    private String signatureMechanism = null;
    private String pdfDigestObjectIdentifier = null;
    private String signatureDigestAlg = null;

    public PDFContentSigner(PrivateKey privateKey, Certificate[] signerCertChain, String signatureMechanism,
                            String signatureDigestAlg, String pdfDigestObjectIdentifier) throws Exception {
        this.privateKey = privateKey;
        this.signerCertChain = signerCertChain;
        this.signatureMechanism = signatureMechanism;
        this.pdfDigestObjectIdentifier = pdfDigestObjectIdentifier;
        this.signatureDigestAlg = signatureDigestAlg;
        this.userCert = (X509Certificate) signerCertChain[0];
    }

    public Certificate[] getCertificateChain() {
        return this.signerCertChain;
    }

    public CMSSignedData getCMSSignedData(String eContentType, CMSProcessable content, boolean encapsulate, Provider sigProvider,
            boolean addDefaultAttributes, List<SignerInfo> signerInfoList) throws NoSuchAlgorithmException, CMSException, Exception {
        // TODO if (signerInfs.isEmpty()){
        //            /* RFC 3852 5.2
        //             * "In the degenerate case where there are no signers, the
        //             * EncapsulatedContentInfo value being "signed" is irrelevant.  In this
        //             * case, the content type within the EncapsulatedContentInfo value being
        //             * "signed" MUST be id-data (as defined in section 4), and the content
        //             * field of the EncapsulatedContentInfo value MUST be omitted."
        //             */
        //            if (encapsulate) {
        //                throw new IllegalArgumentException("no signers, encapsulate must be false");
        //            } if (!DATA.equals(eContentType)) {
        //                throw new IllegalArgumentException("no signers, eContentType must be id-data");
        //            }
        //        }
        //        if (!DATA.equals(eContentType)) {
        //            /* RFC 3852 5.3
        //             * [The 'signedAttrs']...
        //             * field is optional, but it MUST be present if the content type of
        //             * the EncapsulatedContentInfo value being signed is not id-data.
        //             */
        //            // TODO signedAttrs must be present for all signers
        //        }
        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        digests.clear();  // clear the current preserved digest state
        Iterator it = _signers.iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            digestAlgs.add(CMSUtils.fixAlgID(signer.getDigestAlgorithmID()));
            signerInfos.add(signer.toSignerInfo());
        }
        boolean isCounterSignature = (eContentType == null);
        ASN1ObjectIdentifier contentTypeOID = isCounterSignature ?
            CMSObjectIdentifiers.data : new ASN1ObjectIdentifier(eContentType);
        for(SignerInfo signerInfo : signerInfoList) {
            digestAlgs.add(signerInfo.getDigestAlgorithm());
            signerInfos.add(signerInfo);
        }
        ASN1Set certificates = null;
        if (!certs.isEmpty()) certificates = CMSUtils.createBerSetFromList(certs);
        ASN1Set certrevlist = null;
        if (!crls.isEmpty()) certrevlist = CMSUtils.createBerSetFromList(crls);
        ASN1OctetString octs = null;
        if (encapsulate && content != null) {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            content.write(bOut);
            octs = new BERConstructedOctetString(bOut.toByteArray());
        }
        ContentInfo encInfo = new ContentInfo(contentTypeOID, octs);
        SignedData  sd = new SignedData(new DERSet(digestAlgs), encInfo, certificates, certrevlist, new DERSet(signerInfos));
        ContentInfo contentInfo = new ContentInfo(CMSObjectIdentifiers.signedData, sd);
        return new CMSSignedData(content, contentInfo);
    }

    public CMSSignedData genSignedData(byte[] signatureHash, CMSAttributeTableGenerator unsAttr) throws Exception {
        CMSProcessable content = new CMSProcessableByteArray(signatureHash);
        ByteArrayOutputStream out = null;
        if (content != null) {
            out = new ByteArrayOutputStream();
            content.write(out);
            out.close();
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
        MessageDigest softwareDigestEngine = MessageDigest.getInstance(signatureDigestAlg);
        int bytesRead;
        byte[] dataBuffer = new byte[4096];
        while ((bytesRead = bais.read(dataBuffer)) >= 0) {
          softwareDigestEngine.update(dataBuffer, 0, bytesRead);
        }
        byte[] hash = softwareDigestEngine.digest();
        CertStore certsAndCRLs = CertStore.getInstance(CERT_STORE_TYPE,
                new CollectionCertStoreParameters(Arrays.asList(signerCertChain)), ContextVS.PROVIDER);
        addCertificatesAndCRLs(certsAndCRLs);
    	CMSAttributeTableGenerator sAttr = new DefaultSignedAttributeTableGenerator();
        ASN1ObjectIdentifier contentTypeOID = new ASN1ObjectIdentifier(CMSSignedGenerator.DATA);
        Map parameters = getBaseParameters(contentTypeOID,  new AlgorithmIdentifier(new DERObjectIdentifier(
                pdfDigestObjectIdentifier), new DERNull()), hash);
        AttributeTable attributeTable = sAttr.getAttributes(Collections.unmodifiableMap(parameters));
        //String signatureHashStr = new String(Base64.encode(signatureHash));
        JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder =  new JcaSimpleSignerInfoGeneratorBuilder();
        jcaSignerInfoGeneratorBuilder = jcaSignerInfoGeneratorBuilder.setProvider(ContextVS.PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(attributeTable);
        jcaSignerInfoGeneratorBuilder.setUnsignedAttributeGenerator(unsAttr);
        SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(
                signatureMechanism, privateKey, userCert);
        SignerInfo signerInfo = signerInfoGenerator.generate(contentTypeOID);
        List<SignerInfo> signerInfoList = new ArrayList<SignerInfo>();
        signerInfoList.add(signerInfo);
        log.info(" -- userCert: " + userCert.getSubjectDN().getName());
        CMSSignedData signedData = getCMSSignedData(CMSSignedGenerator.DATA, content, true,
                CMSUtils.getProvider("BC"), true, signerInfoList);
        return signedData;
    }

}