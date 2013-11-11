package org.votingsystem.accesscontrol.service

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Properties;

import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.mail.Header;
import javax.mail.Multipart
import javax.mail.Session;
import javax.mail.internet.MimeMultipart;

import org.springframework.beans.factory.InitializingBean
import org.votingsystem.util.*
import org.votingsystem.accesscontrol.model.*
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.springframework.context.ApplicationContext;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import java.security.Key
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.security.cert.CertPathValidatorException
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.mail.smime.SMIMEEnveloped
import org.bouncycastle.mail.smime.SMIMESigned
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.asn1.x509.X509Extensions

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSEnvelopedData
import org.bouncycastle.cms.CMSEnvelopedDataParser
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSTypedStream
import org.bouncycastle.cms.KeyTransRecipientId
import org.bouncycastle.cms.Recipient
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation
import org.bouncycastle.cms.RecipientInformationStore
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.cms.bc.BcRSAKeyTransRecipientInfoGenerator;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.openssl.PEMWriter
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.test.FixedSecureRandom

import javax.mail.BodyPart

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*
* What is triple-DES -> http://www.rsa.com/rsalabs/node.asp?id=2231
* http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
*/
class EncryptionService {

	static scope = "prototype"
	
	private static final String BC = BouncyCastleProvider.PROVIDER_NAME
	
	def grailsApplication;
	def messageSource
	private X509Certificate serverCert;
	private PrivateKey serverPrivateKey;
	private Session session
	private Recipient recipient;
	private RecipientId recId;
	
	//@Override
	public void afterPropertiesSet() throws Exception {
		log.debug(" - afterPropertiesSet - ")
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(aliasClaves);
		serverCert = (X509Certificate)chain[0]
		recId = new JceKeyTransRecipientId(serverCert)
		serverPrivateKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray())
		recipient = new JceKeyTransEnvelopedRecipient(serverPrivateKey).setProvider(BC)
		Properties props = System.getProperties();
		// Get a Session object with the default properties.
		session = Session.getDefaultInstance(props, null);
	}
	
	
	public ResponseVS encryptFile(File fileToEncrypt, File encryptedFile,
			X509Certificate receiverCert) throws Exception {
		log.debug(" - encryptFile(...)");
		try {
			MimeMessage mimeMessage = new MimeMessage(getSession());
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			FileDataSource fds = new FileDataSource(fileToEncrypt);
			mimeBodyPart.setDataHandler(new DataHandler(fds));
			mimeBodyPart.setFileName(fds.getName());
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(mimeBodyPart);
			mimeMessage.setContent(multipart);
			// set the Date: header
			//mimeMessage.setSentDate(new Date());
			SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
			encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
							receiverCert).setProvider(BC));
			/* Encrypt the message */
			MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
					new JceCMSContentEncryptorBuilder(
					CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());
			encryptedPart.writeTo(new FileOutputStream(encryptedFile));
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				file:encryptedFile)
			
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				message:ex.getMessage())
		}
	}
	
	public ResponseVS encryptMessage(byte[] bytesToEncrypt,
		PublicKey publicKey) throws Exception {
				log.debug("--- - encryptMessage(...) - ");
		try {
			MimeBodyPart mimeMessage = new MimeBodyPart();
			mimeMessage.setText(new String(bytesToEncrypt));

			// set the Date: header
			//mimeMessage.setSentDate(new Date());
			SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
			encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
					"".getBytes(), publicKey).setProvider(BC));
			/* Encrypt the message */
			MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
					new JceCMSContentEncryptorBuilder(
					CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());
			ByteArrayOutputStream baos = new ByteArrayOutputStream()
			encryptedPart.writeTo(baos);
			byte[] result = baos.toByteArray()
			baos.close();
			
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				messageBytes:result)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				message:ex.getMessage())
		}
	}
		
	/**
	 * Method to decrypt files attached to SMIME (not signed) messages 
	 */
	public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
		log.debug " - decryptMessage - "
		//log.debug "decryptMessage - encryptedFile: ${new String(encryptedFile)} "
		try {
			MimeMessage msg = new MimeMessage(getSession(), 
				new ByteArrayInputStream(encryptedFile));
			SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
			RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
			RecipientInformation        recipientInfo = recipients.get(getRecipientId());
			RecipientId recipientId = null;
			if(recipientInfo.getRID() != null) {
				recipientId = recipientInfo.getRID();
				log.debug(" - decryptMessage - recipientId.getSerialNumber(): " + recipientId.getSerialNumber());
			}
			MimeBodyPart mimeMessage = SMIMEUtil.toMimeBodyPart(
				recipientInfo.getContent(getRecipient()));
			
			/*ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mimeMessage.writeTo(baos)			
			log.debug(" mimeMessage: ${new String(baos.toByteArray())}")*/

			Object messageContent = mimeMessage.getContent()
			byte[] messageContentBytes = null
			//log.debug(" messageContent class: ${messageContent?.getClass()}")
			if(messageContent instanceof MimeMultipart) {
				MimeMultipart mimeMultipart = (MimeMultipart)messageContent
				BodyPart bodyPart = mimeMultipart.getBodyPart(0)
				InputStream stream = bodyPart.getInputStream();
				ByteArrayOutputStream bodyPartOutputStream = new ByteArrayOutputStream();
				byte[] buf =new byte[2048];
				int len;
				while((len = stream.read(buf)) > 0){
					bodyPartOutputStream.write(buf,0,len);
				}
				stream.close();
				bodyPartOutputStream.close();
				messageContentBytes = bodyPartOutputStream.toByteArray()
			} else if(messageContent instanceof byte[]) {
				messageContentBytes = messageContent
			} else if(messageContent instanceof String) {
				//log.debug(" messageContent: ${messageContent}")
				String[] votingHeaders = mimeMessage.getHeader("votingSystemMessageType")
				log.debug(" - votingHeaders: ${votingHeaders}")
				String votingSystemFile = null
				if(votingHeaders && votingHeaders.length > 0)
					votingSystemFile = mimeMessage.getHeader("votingSystemMessageType")[0]
				if(votingSystemFile) {
					if("voteCsr".equals(votingSystemFile)) {
						messageContentBytes = ((String)messageContent).getBytes()
					} else if("SignedPDF".equals(votingSystemFile)) {
						messageContentBytes = Base64.decode((String)messageContent);
					}
				} else messageContentBytes = (String)messageContent;
			}
			return new ResponseVS(statusCode: ResponseVS.SC_OK,
				messageBytes:messageContentBytes)
		} catch(CMSException ex) {
			log.error (ex.getMessage(), ex)
			//log.error(" --- encryptedFile: ${new String(encryptedFile)}")
			return new ResponseVS(message:messageSource.getMessage(
				'encryptedMessageErrorMsg', null, locale),
				statusCode:ResponseVS.SC_ERROR_PETICION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			//log.error(" --- encryptedFile: ${new String(encryptedFile)}")
			return new ResponseVS(statusCode: ResponseVS.SC_ERROR_PETICION,
				message:ex.getMessage())
		}
	}

	public ResponseVS encryptToCMS(byte[] dataToEncrypt,
		X509Certificate reciCert) throws Exception {
		log.debug(" - encryptToCMS")
		CMSEnvelopedDataStreamGenerator dataStreamGen = new CMSEnvelopedDataStreamGenerator();
		dataStreamGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(reciCert).setProvider(BC));
		ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
		OutputStream out = dataStreamGen.open(bOut,
				new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).
				setProvider(BC).build());
		out.write(dataToEncrypt);
		out.close();
		byte[] result = bOut.toByteArray();
		byte[] base64EncryptedDataBytes = Base64.encode(result);
		return new ResponseVS(messageBytes:base64EncryptedDataBytes,
			statusCode:ResponseVS.SC_OK);
	}
	
	public byte[] decryptCMS(PrivateKey privateKey, 
		byte[] base64EncryptedData) throws Exception {
		log.debug(" - decryptCMS")
		
		byte[] cmsEncryptedData = Base64.decode(base64EncryptedData);
		
        CMSEnvelopedDataParser     ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection                 c = recipients.getRecipients();
        Iterator                   it = c.iterator();

        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(new JceKeyTransEnvelopedRecipient(privateKey).setProvider(BC));
            InputStream           dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[]                buf = new byte[4096];
s
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
		
	/**
	 * Method to encrypt SMIME signed messages
	 */
	ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt,
		X509Certificate receiverCert, Locale locale) throws Exception {
		log.debug(" - encryptSMIMEMessage(...) ");
		//If the message isn't recreated there can be problems with
		//multipart boundaries. TODO
		SMIMEMessageWrapper msgToEncrypt = new SMIMEMessageWrapper(
				new ByteArrayInputStream(bytesToEncrypt));
		try {
			//String str1 = new String(msgToEncrypt.getBytes())
			//log.debug(" - encryptSMIMEMessage(...) str1: " + str1);
			SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
			encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
				receiverCert).setProvider(BC));
			/* Encrypt the message */
			MimeBodyPart encryptedPart = encrypter.generate(msgToEncrypt,
				new JceCMSContentEncryptorBuilder(
				CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());
			/* Set all original MIME headers in the encrypted message */
			Enumeration headers = msgToEncrypt.getAllHeaderLines();
			while (headers.hasMoreElements()) {
				String headerLine = (String)headers.nextElement();
				//log.debug(" - headerLine: ${headerLine}");
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
			byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.
			String digestStr = new String(Base64.encode(digestBytes));
			encryptedMessage.addHeaderLine("SignedMessageDigest: " + digestStr);*/
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			encryptedPart.writeTo(baos);
			return new ResponseVS(messageBytes:baos.toByteArray(),
				statusCode:ResponseVS.SC_OK);
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(message:messageSource.getMessage(
				'error.encryptErrorMsg', null, locale),
				statusCode:ResponseVS.SC_ERROR_PETICION)
		}
	}		


	/**
	 * Method to decrypt SMIME signed messages
	 */
	ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
		log.debug(" - decryptSMIMEMessage - ")
		SMIMEMessageWrapper smimeMessageReq = null;
		RecipientInformationStore recipients = null;
		RecipientInformation recipientInfo = null
		byte[] messageContentBytes = null
		try {
			MimeMessage msg = new MimeMessage(getSession(), 
				new ByteArrayInputStream(encryptedMessageBytes));
			SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);		 
			recipients = smimeEnveloped.getRecipientInfos();
			recipientInfo = recipients.get(getRecipientId());

			/*RecipientId recipientRID = null;
			if(recipient.getRID() != null) {
				recipientRID = recipient.getRID();
				log.debug(" -- recipientRID.getSerialNumber(): " + recipientRID.getSerialNumber());
				if(recipient.getRID().getCertificate() != null) {
					log.debug(" -- recipient: " + recipient.getRID().getCertificate().getSubjectDN().toString());
				} else log.debug(" -- recipient.getRID().getCertificate() NULL");
			} else log.debug(" -- getRID NULL");
			MimeBodyPart res = SMIMEUtil.toMimeBodyPart(
				 recipient.getContent(new JceKeyTransEnvelopedRecipient(serverPrivateKey).setProvider(BC)));*/
			if(!recipientInfo) {
				String messsage = messageSource.getMessage(
					'encryptedMessageContentNotFoundErrorMsg', null, locale)
				log.error("${messsage} - RecipientId: ${getRecipientId()?.toString()}")
				return new ResponseVS(message:messsage, 
					statusCode:ResponseVS.SC_ERROR)
			}
			messageContentBytes =  recipientInfo.getContent(getRecipient())
			//MimeBodyPart mimeBodyPart =  SMIMEUtil.toMimeBodyPart(messageContentBytes);
			
			smimeMessageReq = new SMIMEMessageWrapper(
				new ByteArrayInputStream(messageContentBytes));
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			log.error(" ---> Expected recipient Info: ${getRecipientId()}")
			Iterator iterator = recipients.recipients.iterator()
			while(iterator.hasNext()) {
				RecipientInformation msgRecInfo = iterator.next();
				log.error("Msg recipient Info: ${msgRecInfo.getRID()}")
			}
			String msg = null
			if(messageContentBytes != null) {
				msg = new String(messageContentBytes);	
			} else msg = new String(encryptedMessageBytes);
			log.debug(" - decryptSMIMEMessage - msg: '${msg}'");
			return new ResponseVS(message:messageSource.getMessage(
				'encryptedMessageErrorMsg', null, locale),
				statusCode:ResponseVS.SC_ERROR_PETICION)
		}
		return new ResponseVS(smimeMessage:smimeMessageReq,
			statusCode:ResponseVS.SC_OK)
	}
	
	private Session getSession() {
		return session
	}
	
	private Recipient getRecipient() {
		return recipient;	
	}
	
	
	private RecipientId getRecipientId() {
		if(recId == null) afterPropertiesSet()
		return recId;
	}
	
}