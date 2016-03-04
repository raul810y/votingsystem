package org.votingsystem.signature.util;

import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.Strings;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 *
 * What is triple-DES -> http://www.rsa.com/rsalabs/node.asp?id=2231
 * http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
 */
public class Encryptor {
 
    private static Logger log = Logger.getLogger(Encryptor.class.getName());

    private static final int ITERATION_COUNT = 1024;
    private static final int KEY_LENGTH = 128; // 192 and 256 bits may not be available

    private Recipient recipient;
    private RecipientId recipientId;
    private PrivateKey privateKey;

    public Encryptor(X509Certificate localCert, PrivateKey localPrivateKey) {
        this.privateKey = localPrivateKey;
        recipientId = new JceKeyTransRecipientId(localCert);
        recipient = new JceKeyTransEnvelopedRecipient(localPrivateKey).setProvider(ContextVS.PROVIDER);
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        MimeBodyPart mimeMessage = new MimeBodyPart();
        mimeMessage.setText(new String(bytesToEncrypt));
        //mimeMessage.setSentDate(new Date());// set the Date: header
        SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
        encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                "".getBytes(), publicKey).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encryptor.generate(mimeMessage, new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        baos.close();
        return baos.toByteArray();
    }

    public static byte[] encryptMessage(byte[] text, X509Certificate receiverCert, Header... headers) throws Exception {
        MimeMessage mimeMessage = new MimeMessage(ContextVS.MAIL_SESSION);
        mimeMessage.setText(new String(text, "UTF-8"));
        // set the Date: header
        //mimeMessage.setSentDate(new Date());
        if (headers != null) {
            for(Header header : headers) {
                if(header != null) mimeMessage.setHeader(header.getName(), header.getValue());
            }
        }
        SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
        encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encryptor.generate(mimeMessage,
                new JceCMSContentEncryptorBuilder(
                        CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        baos.close();
        return  baos.toByteArray();
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public byte[] decryptMessage (byte[] encryptedFile) throws Exception {
        MimeMessage msg = new MimeMessage(ContextVS.MAIL_SESSION, new ByteArrayInputStream(encryptedFile));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = recipients.get(recipientId);
        RecipientId messageRecipientId = null;
        if(recipientInfo != null && recipientInfo.getRID() != null) {
            messageRecipientId = recipientInfo.getRID();
            log.info("messageRecipientId.getSerialNumber(): " + messageRecipientId.getSerialNumber());
        } else throw new ExceptionVS("No message found for recipientId: " + recipientId.getSerialNumber());
        MimeBodyPart mimeMessage = SMIMEUtil.toMimeBodyPart(recipientInfo.getContent(recipient));
        			/*ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mimeMessage.writeTo(baos)
			log.info(" mimeMessage: ${new String(baos.toByteArray())}")*/
        Object messageContent = mimeMessage.getContent();
        byte[] messageContentBytes = null;
        //log.info(" messageContent class: ${messageContent?.getClass()}")
        if(messageContent instanceof MimeMultipart) {
            MimeMultipart mimeMultipart = (MimeMultipart)messageContent;
            BodyPart bodyPart = mimeMultipart.getBodyPart(0);
            InputStream stream = bodyPart.getInputStream();
            ByteArrayOutputStream bodyPartOutputStream = new ByteArrayOutputStream();
            byte[] buf =new byte[2048];
            int len;
            while((len = stream.read(buf)) > 0){ bodyPartOutputStream.write(buf,0,len); }
            stream.close();
            bodyPartOutputStream.close();
            messageContentBytes = bodyPartOutputStream.toByteArray();
        } else if(messageContent instanceof byte[]) {
            messageContentBytes = (byte[]) messageContent;
        } else if(messageContent instanceof String) {
            //log.info(" messageContent: ${messageContent}")
            String[] votingHeaders = mimeMessage.getHeader("votingSystemMessageType");
            String encodedContentType = null;
            if(votingHeaders != null && votingHeaders.length > 0)
                encodedContentType = mimeMessage.getHeader("votingSystemMessageType")[0];
            if(encodedContentType != null) {
                if(ContextVS.BASE64_ENCODED_CONTENT_TYPE.equals(encodedContentType)) {
                    messageContentBytes = Base64.getDecoder().decode((String) messageContent);
                } else log.log(Level.SEVERE, "### unknown  votingSystemMessageType: " + encodedContentType);
            } else messageContentBytes = messageContent.toString().getBytes();
        }
        return messageContentBytes;
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    public ResponseVS encryptSMIME(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        //If the message isn't recreated there can be problems with multipart boundaries. TODO
        SMIMEMessage msgToEncrypt = new SMIMEMessage(bytesToEncrypt);
        SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
        encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(receiverCert).setProvider("BC"));
			/* Encrypt the message */
        MimeBodyPart encryptedPart = encryptor.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        // Create a new MimeMessage that contains the encrypted and signed content
			/* Set all original MIME headers in the encrypted message */
        Enumeration headers = msgToEncrypt.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            //log.info(" - headerLine: ${headerLine}");
				/*
				* Make sure not to override any content-* headers from the
				* original message
				*/
            if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
                encryptedPart.addHeaderLine(headerLine);
            }
        }
        /*SignerInformationStore  signers =
            msgToEncrypt.getSmimeSigned().getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.*/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        return new ResponseVS(ResponseVS.SC_OK, baos.toByteArray());
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    public ResponseVS decryptSMIME(byte[] encryptedMessageBytes) throws Exception {
        SMIMEMessage smimeMessageReq = null;
        MimeMessage msg = new MimeMessage(ContextVS.MAIL_SESSION, new ByteArrayInputStream(encryptedMessageBytes));
        //String encryptedMessageBytesStr = new String(encryptedMessageBytes);
        //log.info("- decryptSMIME - encryptedMessageBytesStr: " + encryptedMessageBytesStr)
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation recipientInfo = recipients.get(recipientId);
			/*RecipientId recipientRID = null;
			if(recipient.getRID() != null) {
				recipientRID = recipient.getRID();
				log.info(" -- recipientRID.getSerialNumber(): " + recipientRID.getSerialNumber());
				if(recipient.getRID().getCertificate() != null) {
					log.info(" -- recipient: " + recipient.getRID().getCertificate().getSubjectDN().toString());
				} else log.info(" -- recipient.getRID().getCertificate() NULL");
			} else log.info(" -- getRID NULL");
			MimeBodyPart res = SMIMEUtil.toMimeBodyPart(
				 recipient.getContent(new JceKeyTransEnvelopedRecipient(serverPrivateKey).setProvider("BC")));*/
        if(recipientInfo == null) {
            log.log(Level.SEVERE, "Expected recipientId.getSerialNumber(): " + recipientId.getSerialNumber());
            Collection<RecipientInformation> recipientCollection = recipients.getRecipients();
            for(RecipientInformation recipientInf : recipientCollection) {
                log.log(Level.SEVERE, "Encrypted document recipientId.getSerialNumber(): " +
                        recipientInf.getRID().getSerialNumber());
            }
            return new ResponseVS(ResponseVS.SC_ERROR_REQUEST, ContextVS.getMessage("encryptionRecipientErrorMsg"));
        }
        byte[] messageContentBytes =  recipientInfo.getContent(recipient);
        //log.info(" ------- Message Contents: ${new String(messageContentBytes)}");
        smimeMessageReq = new SMIMEMessage(messageContentBytes);
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
        responseVS.setSMIME(smimeMessageReq);
        return responseVS;
    }

    public static byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receptorCert) throws Exception {
        CMSEnvelopedDataStreamGenerator dataStreamGen = new CMSEnvelopedDataStreamGenerator();
        dataStreamGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(receptorCert).
                setProvider(ContextVS.PROVIDER));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = dataStreamGen.open(bOut, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).
                        setProvider(ContextVS.PROVIDER).build());
        out.write(dataToEncrypt);
        out.close();
        return Base64.getEncoder().encode(bOut.toByteArray());
    }

    public static byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey  receptorPublicKey) throws Exception {
        CMSEnvelopedDataStreamGenerator dataStreamGen = new CMSEnvelopedDataStreamGenerator();
        dataStreamGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator("".getBytes(), receptorPublicKey).
                setProvider(ContextVS.PROVIDER));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = dataStreamGen.open(bOut,
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).
                        setProvider(ContextVS.PROVIDER).build());
        out.write(dataToEncrypt);
        out.close();
        return Base64.getEncoder().encode(bOut.toByteArray());
    }

    public byte[] decryptCMS(byte[] base64EncryptedData) throws Exception {
        byte[] cmsEncryptedData = Base64.getDecoder().decode(base64EncryptedData);
        CMSEnvelopedDataParser     ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection                 c = recipients.getRecipients();
        Iterator                   it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.PROVIDER));
            return FileUtils.getBytesFromStream(recData.getContentStream());
        }
        return result;
    }

    public static byte[] decryptCMS (byte[] base64EncryptedData, PrivateKey privateKey) throws CMSException, IOException {
        byte[] cmsEncryptedData = org.bouncycastle.util.encoders.Base64.decode(base64EncryptedData);
        CMSEnvelopedDataParser     ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection                 c = recipients.getRecipients();
        Iterator                   it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.PROVIDER));
            return FileUtils.getBytesFromStream(recData.getContentStream());
        }
        return result;
    }

    public static byte[] encryptSMIME(SMIMEMessage msgToEncrypt, X509Certificate receiverCert) throws Exception {
        /* Create the encryptor */
        SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
        encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encryptor.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(
                        CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = msgToEncrypt.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            log.info("headerLine: " + headerLine);
            /* 
             * Make sure not to override any content-* headers from the
             * original message
             */
            if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
                encryptedPart.addHeaderLine(headerLine);
            }
        }
        /*SignerInformationStore  signers = 
                        msgToEncrypt.getSmimeSigned().getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.*/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }    
       	
    /**
    * Method to decrypt SMIME signed messages
    */
   public static SMIMEMessage decryptSMIME(
           byte[] encryptedMessageBytes, PublicKey  publicKey, 
            PrivateKey receiverPrivateKey) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(decryptMessage(encryptedMessageBytes, receiverPrivateKey));
        return new SMIMEMessage(inputStream);
   }

    /**
     * helper method to decrypt SMIME signed messages
     */
    public static byte[] decryptMessage(byte[] encryptedMessageBytes, PrivateKey receiverPrivateKey) throws Exception {
        log.info("decryptMessage(...) ");
        RecipientId recId = null;
        /*if(receiverCert != null) recId = new JceKeyTransRecipientId(receiverCert);*/
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                receiverPrivateKey).setProvider(ContextVS.PROVIDER);
        MimeMessage msg = new MimeMessage(null,
                new ByteArrayInputStream(encryptedMessageBytes));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = null;
        //if(recId != null) recipientInfo = recipients.get(recId);
        if(recipientInfo == null && recipients.getRecipients().size() == 1) {
            recipientInfo = (RecipientInformation)
                    recipients.getRecipients().iterator().next();
        }
        byte[] messageBytes = recipientInfo.getContent(recipient);
        return messageBytes;
    }
    
    public static byte[] encryptFile(File fileToEncrypt, X509Certificate receiverCert) throws Exception {
        log.info("encryptFile(...)");
        MimeMessage mimeMessage = new MimeMessage(ContextVS.MAIL_SESSION);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(fileToEncrypt);
        mimeBodyPart.setDataHandler(new DataHandler(fds));
        mimeBodyPart.setFileName(fds.getName());
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        mimeMessage.setContent(multipart);
        // set the Date: header
        //mimeMessage.setSentDate(new Date());
        SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
        encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encryptor.generate(mimeMessage, new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }

    public static byte[] decryptCMSStream(PrivateKey privateKey, byte[] cmsEncryptedData) throws Exception {
        CMSEnvelopedDataParser     ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection                 c = recipients.getRecipients();
        Iterator                   it = c.iterator();

        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.PROVIDER));
            InputStream           dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[]                buf = new byte[4096];
            int len = 0;
            while ((len = dataStream.read(buf)) >= 0) {
                    dataOut.write(buf, 0, len);
            }
            dataOut.close();
            result = dataOut.toByteArray();
            //assertEquals(true, Arrays.equals(data, dataOut.toByteArray()));
        }
        return result;
    }
    
    public static byte[] decryptFile (byte[] encryptedFile, PublicKey publicKey, PrivateKey receiverPrivateKey)
            throws Exception {
        RecipientId recId = new KeyTransRecipientId(publicKey.getEncoded());
        Recipient recipient = new JceKeyTransEnvelopedRecipient(receiverPrivateKey).setProvider(ContextVS.PROVIDER);
        MimeMessage msg = new MimeMessage(null, new ByteArrayInputStream(encryptedFile));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = null;
        recipientInfo = recipients.get(recId);
        if(recipientInfo == null && recipients.getRecipients().size() == 1) {
            recipientInfo = (RecipientInformation) recipients.getRecipients().iterator().next();
        }
        RecipientId fileRecipientId = null;
        if(recipientInfo.getRID() != null) {
            fileRecipientId = recipientInfo.getRID();
        }
        return recipientInfo.getContent(recipient);
    }

    public static EncryptedBundle pbeAES_Encrypt(char[] password, byte[] bytesToEncrypt) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = KeyGeneratorVS.INSTANCE.getSalt();
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        return new EncryptedBundle(cipher.doFinal(bytesToEncrypt), iv, salt);
    }

    public static byte[] pbeAES_Decrypt(char[] password, EncryptedBundle bundle) throws
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidKeySpecException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, bundle.salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(bundle.iv));
        return cipher.doFinal(bundle.cipherText);
    }

    /*public static String encryptAES(String messageToEncrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException,
            NoSuchProviderException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessage = cipher.doFinal(messageToEncrypt.getBytes("UTF-8"));
        return new String(org.bouncycastle.util.encoders.Base64.encode(encryptedMessage));
    }

    //decrypts base64 encoded AES message
    public static String decryptAES(String messageToDecrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessageBytes = org.bouncycastle.util.encoders.Base64.decode(messageToDecrypt.getBytes());
        byte[] decryptedBytes = cipher.doFinal(encryptedMessageBytes);
        return new String(decryptedBytes, "UTF8");
    }*/

    //BC provider to avoid key length restrictions on normal jvm
    public static String encryptAES(String messageToEncrypt, AESParams aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getKey().getEncoded());
        ParametersWithIV params = new ParametersWithIV(keyParam, aesParams.getIV().getIV());
        pbbc.init(true, params); //to decrypt put param to false
        byte[] input = messageToEncrypt.getBytes("UTF-8");
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        return new String(org.bouncycastle.util.encoders.Base64.encode(output));
    }

    //BC provider to avoid key length restrictions on normal jvm
    public static String decryptAES(String messageToDecrypt, AESParams aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getKey().getEncoded());
        CipherParameters params = new ParametersWithIV(keyParam, aesParams.getIV().getIV());
        pbbc.init(false, params); //to encrypt put param to true
        byte[] input = org.bouncycastle.util.encoders.Base64.decode(messageToDecrypt.getBytes("UTF-8"));
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        int i = output.length - 1; //remove padding
        while (i >= 0 && output[i] == 0) { --i; }
        return new String(Arrays.copyOf(output, i + 1), "UTF-8");
    }

    public static String encryptRSA(String plainText, PublicKey publicKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = rsaCipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decryptRSA(String encryptedTextBase64, PrivateKey privateKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainText = rsaCipher.doFinal(Base64.getDecoder().decode(encryptedTextBase64));
        return new String(plainText, "UTF-8");
    }

}
