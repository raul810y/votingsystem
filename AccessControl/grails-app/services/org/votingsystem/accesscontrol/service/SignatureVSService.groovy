package org.votingsystem.accesscontrol.service

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.signature.util.SVCertExtensionChecker
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

import javax.mail.Header
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.security.KeyStore
import java.security.cert.*
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass

//class SignatureVSService implements InitializingBean {
class SignatureVSService {

	private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
	
	private SignedMailGenerator signedMailGenerator;
	static Set<X509Certificate> trustedCerts;
	private KeyStore trustedCertsKeyStore
	static HashMap<Long, CertificateVS> trustedCertsHashMap;
	private X509Certificate localServerCertSigner;
	private static HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsHashMap = 
		new HashMap<Long, Set<TrustAnchor>>();
	private static HashMap<Long, Set<TrustAnchor>> controlCenterTrustedAnchorsHashMap =
		new HashMap<Long, Set<TrustAnchor>>();
	def grailsApplication;
	def messageSource
	def csrService;
	def encryptionService;
	def subscriptionVSService
	def timeStampVSService
	def sessionFactory
	boolean testMode = false
	
	public ResponseVS deleteTestCerts () {
		log.debug(" - deleteTestCerts - ")


        /*def d = new DefaultGrailsDomainClass(CertificateVS.class)
        d.persistentProperties.each {
            log.debug("============ ${it}")
        }*/


		int numTestCerts = CertificateVS.countByType(CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST)
		log.debug(" - deleteTestCerts - numTestCerts: ${numTestCerts}") 
		long begin = System.currentTimeMillis()
		def criteria = CertificateVS.createCriteria()
		def testCerts = criteria.scroll {
			eq("type", CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST)
		}   
		while (testCerts.next()) {
			CertificateVS cert = (CertificateVS) testCerts.get(0);

			int numCerts = CertificateVS.countByAuthorityCertificateVS(cert)
			def userCertCriteria = CertificateVS.createCriteria()
			def userTestCerts = userCertCriteria.scroll {
				eq("authorityCertificateVS", cert)
			}
			while (userTestCerts.next()) { 
				CertificateVS userCert = (CertificateVS) userTestCerts.get(0);
				userCert.delete()
				if((userTestCerts.getRowNumber() % 100) == 0) {
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
					log.debug(" - processed ${userTestCerts.getRowNumber()}/${numCerts} user certs from auth. cert ${cert.id}");
				}
				
			}
			CertificateVS.withTransaction {
				cert.delete()
			}
		}
		afterPropertiesSet();
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	//@Override
	public SignedMailGenerator afterPropertiesSet() throws Exception {
		log.debug(" - afterPropertiesSet - ")
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStoreFile), 
			aliasClaves, password.toCharArray(), ContextVS.SIGN_MECHANISM);
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);
		byte[] pemCertsArray
		trustedCerts = new HashSet<X509Certificate>()
		for (int i = 0; i < chain.length; i++) {
			log.debug "Adding local kesystore cert '${i}' -> 'SubjectDN: ${chain[i].getSubjectDN()}'"
			trustedCerts.add(chain[i])
			if(!pemCertsArray) pemCertsArray = CertUtil.getPEMEncoded (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.getPEMEncoded (chain[i]))
		}
		
		localServerCertSigner = (X509Certificate) ks.getCertificate(aliasClaves);
		trustedCerts.add(localServerCertSigner)

		File certChainFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certChainPath).getFile();
		certChainFile.createNewFile()
		certChainFile.setBytes(pemCertsArray)
		initCertAuthorities();
        return signedMailGenerator;
	}
	
	public boolean isSystemSignedMessage(Set<UserVS> signers) {
		boolean result = false
		log.debug "isSystemSignedMessage - localServerCert num. serie: ${localServerCertSigner.getSerialNumber().longValue()}"
		signers.each {
			long signerId = ((UserVS)it).getCertificate().getSerialNumber().longValue()
			log.debug " --- num serie signer: ${signerId}"
			if(signerId == localServerCertSigner.getSerialNumber().longValue()) result = true;
		}
		return result
	}
	
	public Set<X509Certificate> getTrustedCerts() {
		if(!trustedCerts || trustedCerts.isEmpty()) {
			afterPropertiesSet()
		}
		return trustedCerts;
	}
	
	public ResponseVS getEventTrustedCerts(EventVS event, Locale locale) {
		log.debug("getEventTrustedCerts")
		if(!event) return new ResponseVS(ResponseVS.SC_ERROR)
		CertificateVS eventVSCertificateVS = CertificateVS.findWhere(eventVSElection:event, state:CertificateVS.State.OK,
			type:CertificateVS.Type.VOTEVS_ROOT)
		if(!eventVSCertificateVS) {
			String msg = messageSource.getMessage('eventWithoutCAErrorMsg', [event.id].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT CA CERT -> '${msg}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:msg, type:TypeVS.VOTE_ERROR, eventVS:event)
		}
		X509Certificate certCAEventVS = CertUtil.loadCertificateFromStream (
			new ByteArrayInputStream(eventVSCertificateVS.content))
		Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>()
		eventTrustedCerts.add(certCAEventVS)
		return new ResponseVS(statusCode:ResponseVS.SC_OK, data:eventTrustedCerts)
	}
	
	public ResponseVS getEventTrustedAnchors(EventVS event, Locale locale) {
		log.debug("getEventTrustedAnchors")
		if(!event) return new ResponseVS(ResponseVS.SC_ERROR)
		Set<TrustAnchor> eventTrustAnchors = eventTrustedAnchorsHashMap.get(event.id)
		ResponseVS responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, data:eventTrustAnchors)
		if(!eventTrustAnchors) {
			CertificateVS eventVSCertificateVS = CertificateVS.findWhere(
				eventVSElection:event, state:CertificateVS.State.OK,
				type:CertificateVS.Type.VOTEVS_ROOT)
			if(!eventVSCertificateVS) {
				String msg = messageSource.getMessage('eventWithoutCAErrorMsg', [event.id].toArray(), locale)
				log.error ("validateVoteCerts - ERROR EVENT CA CERT -> '${msg}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.VOTE_ERROR, eventVS:event)
			}
			X509Certificate certCAEventVS = CertUtil.loadCertificateFromStream (
				new ByteArrayInputStream(eventVSCertificateVS.content))
			TrustAnchor anchor = new TrustAnchor(certCAEventVS, null);
			eventTrustAnchors = new HashSet<TrustAnchor>()
			eventTrustAnchors.add(anchor)
			eventTrustedAnchorsHashMap.put(event.id, eventTrustAnchors)
			responseVS.data = eventTrustAnchors
			
		}
		return responseVS
	}
	
	public KeyStore getTrustedCertsKeyStore() {
		if(!trustedCertsKeyStore ||
			trustedCertsKeyStore.size() != trustedCerts.size()) {
			trustedCertsKeyStore = KeyStore.getInstance("JKS");
			trustedCertsKeyStore.load(null, null);
			Set<X509Certificate> trustedCertsSet = getTrustedCerts()
			log.debug "trustedCerts.size: ${trustedCertsSet.size()}"
			for(X509Certificate certificate:trustedCertsSet) {
				trustedCertsKeyStore.setCertificateEntry(certificate.getSubjectDN().toString(), certificate);
			}
		}
		return trustedCertsKeyStore;
	}
	
	def initCertAuthorities() {
		try {
			trustedCertsHashMap = new HashMap<Long, CertificateVS>();
			File directory = grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
			
			File[] acFiles = directory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return fileName.startsWith("AC_") && fileName.endsWith(".pem");
				}
			  });
		   Set<X509Certificate> fileSystemCerts = new HashSet<X509Certificate>()
			for(File caFile:acFiles) {
				fileSystemCerts.addAll(CertUtil.fromPEMToX509CertCollection(FileUtils.getBytesFromFile(caFile)));
			}
			for(X509Certificate x509Certificate:fileSystemCerts) {
				long numSerie = x509Certificate.getSerialNumber().longValue()
				log.debug "initCertAuthorities - checking - ${x509Certificate?.getSubjectDN()} --- numSerie:${numSerie}"
                CertificateVS certificate = null
				CertificateVS.withTransaction { certificate = CertificateVS.findBySerialNumber(numSerie)}
				if(!certificate) {
					boolean isRoot = CertUtil.isSelfSigned(x509Certificate)
					certificate = new CertificateVS(isRoot:isRoot, type:CertificateVS.Type.CERTIFICATE_AUTHORITY,
						state:CertificateVS.State.OK, content:x509Certificate.getEncoded(), serialNumber:numSerie,
                        validFrom:x509Certificate.getNotBefore(), validTo:x509Certificate.getNotAfter())
					certificate.save()
					log.debug "initCertAuthorities - ADDED NEW CA CERT certificateVS.id:'${certificate?.id}'"
				} else {
					if(CertificateVS.State.OK != certificate.state) {
						log.error "File system athority cert '${x509Certificate?.getSubjectDN()}' " +
                                " with certificateVS.id:'${certificate?.id}' state is '${certificate.state}'"
					}
				}
			}
			CertificateVS.withTransaction {
				def criteria = CertificateVS.createCriteria()
				def trustedCertsDB = criteria.list {
					eq("state", CertificateVS.State.OK)
					or {
						eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
						if(EnvironmentVS.DEVELOPMENT  ==  ApplicationContextHolder.getEnvironment()) {
							eq("type", CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST)
						}
					}
				}
				trustedCertsDB.each { certificate ->
					ByteArrayInputStream bais = new ByteArrayInputStream(certificate.content)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					trustedCerts.add(certX509)
					trustedCertsHashMap.put(certX509?.getSerialNumber()?.longValue(), certificate)
				}
			}
			log.debug("trustedCerts.size(): ${trustedCerts?.size()}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"CA Authorities initialized")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
	}
	
	def checkCancelledCerts () {
		log.debug "checkCancelledCerts - checkCancelledCerts"
		File directory = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
		String cancelSufix = "_CANCELLED"
		directory.eachFile() { file ->
			String fileName = file.getName().toUpperCase()
			if(fileName.endsWith(cancelSufix)) {
				int idx = fileName.indexOf(cancelSufix)
				fileName = fileName.substring(0, idx);
				if(fileName.endsWith("JKS")) {
					log.debug ("--- cancelando JKS -> " + fileName)
					KeyStore ks = KeyStore.getInstance("JKS");
					String password = grailsApplication.config.VotingSystem.signKeysPassword
					String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
					ks.load(new FileInputStream(file), password.toCharArray());
					java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);				
					for (int i = 0; i < chain.length; i++) {
						X509Certificate cert = chain[i]
						cancelCert(cert.getSerialNumber().longValue())
					}
					file.delete();
				} else if (fileName.endsWith("PEM")) {
					log.debug ("--- cancelando PEM -> " + fileName)
					Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
						FileUtils.getBytesFromFile(file))
					for (X509Certificate cert :certificates) {
						cancelCert(cert.getSerialNumber().longValue())
					}
					file.delete();
				}
			}
		}
	}
	
	private void cancelCert(long numSerieCert) {
		log.debug "cancelCert - numSerieCert: ${numSerieCert}"
		CertificateVS.withTransaction {
			CertificateVS certificate = CertificateVS.findWhere(serialNumber:numSerieCert)
			if(certificate) {
				log.debug "Comprobando certificateVS.id '${certificate?.id}'  --- "
				if(CertificateVS.State.OK == certificate.state) {
					certificate.cancelDate = new Date(System.currentTimeMillis());
					certificate.state = CertificateVS.State.CANCELLED;
					certificate.save()
					log.debug "cancelado certificateVS '${certificate?.id}'"
				} else log.debug "El certificateVS.id '${certificate?.id}' ya estaba cancelado"
			} else log.debug "No hay ningún certificateVS con num. serie '${numSerieCert}'"
		}
	}
	
	public File getSignedFile (String fromUser, String toUser,
		String textToSign, String subject, Header header) {
		log.debug "getSignedFile - textToSign: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		resultFile.deleteOnExit();
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}
		
	public byte[] getSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject, Header header) {
		log.debug "getSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mimeMessage.writeTo(baos);
		baos.close();
		return baos.toByteArray();
	}
		
	public synchronized SMIMEMessageWrapper getMultiSignedMimeMessage (
		String fromUser, String toUser,	final SMIMEMessageWrapper smimeMessage, String subject) {
		log.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		} 
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setHeader("To", toUser)
		}
		SMIMEMessageWrapper multiSignedMessage = getSignedMailGenerator().genMultiSignedMessage(smimeMessage, subject);
		return multiSignedMessage
	}
	
	/*
	 * Método para poder añadir certificados de confianza en las pruebas de carga.
	 * El procedimiento para añadir una autoridad certificadora consiste en 
	 * añadir el certificateVS en formato pem en el directorio ./WEB-INF/cms
	 */
	public ResponseVS addCertificateAuthority (byte[] caPEM, Locale locale)  {
		log.debug("addCertificateAuthority");
		if(grails.util.Environment.PRODUCTION  ==  grails.util.Environment.current) {
			log.debug(" ### ADDING CERTS NOT ALLOWED IN PRODUCTION ENVIRONMENTS ###")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message: messageSource.getMessage('serviceDevelopmentModeMsg', null, locale))
		}
		if(!caPEM) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
			message: messageSource.getMessage('nullCertificateErrorMsg', null, locale))
		try {
			Collection<X509Certificate> certX509CertCollection = CertUtil.fromPEMToX509CertCollection(caPEM)
			for(X509Certificate cert: certX509CertCollection) {
				log.debug(" ------- addCertificateAuthority - adding cert: ${cert.getSubjectDN()}" + 
					" - serial number: ${cert.getSerialNumber()}");
				CertificateVS certificate = null
				CertificateVS.withTransaction {
					certificate = CertificateVS.findBySerialNumber(
						cert?.getSerialNumber()?.longValue())
					if(!certificate) {
						boolean isRoot = CertUtil.isSelfSigned(cert)
						certificate = new CertificateVS(isRoot:isRoot,
							type:CertificateVS.Type.CERTIFICATE_AUTHORITY_TEST,
							state:CertificateVS.State.OK,
							content:cert.getEncoded(),
							serialNumber:cert.getSerialNumber()?.longValue(),
							validFrom:cert.getNotBefore(),
							validTo:cert.getNotAfter())
						certificate.save()
						trustedCertsHashMap.put(cert?.getSerialNumber()?.longValue(), certificate)
					}
				} 
				trustedCerts.addAll(certX509CertCollection)
				log.debug "Almacenada Autoridad Certificadora de pruebas con id:'${certificate?.id}'"
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, 
				message:messageSource.getMessage('cert.newCACertMsg', null, locale))
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
	}
	
	public CertificateVS getCACertificate(long numSerie) {
		log.debug("getCACertificate - numSerie: '${numSerie}'")
		return trustedCertsHashMap.get(numSerie)
	}
		
	public ResponseVS validateSMIME(SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIME")
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message)
		}
		return validateSignersCertificate(messageWrapper, locale)
	}
		
	public ResponseVS validateSignersCertificate(SMIMEMessageWrapper messageWrapper, Locale locale) {
		Set<UserVS> signersVS = messageWrapper.getSigners();
		if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
			messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
		log.debug("validateSignersCertificate - number of signers: ${signersVS.size()}")
		Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        String signerNIF = messageWrapper.getSigner().getNif()
		for(UserVS userVS: signersVS) {
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(userVS.getCertificate(),
                        getTrustedCerts(), false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				userVS.setCertificateCA(trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue()))
				log.debug("validateSignersCertificate - CertificateVS de userVS emitido por: " +
						certCaResult?.getSubjectDN()?.toString() +
                        " - serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
				ResponseVS responseVS = subscriptionVSService.checkUser(userVS, locale)
				if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
				if(userVS.getTimeStampToken() != null) {
					ResponseVS timestampValidationResp = timeStampVSService.validateToken(
						userVS.getTimeStampToken(), locale)
					log.debug("validateSignersCertificate - timestampValidationResp - " +
                        "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
					if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
						log.error("validateSignersCertificate - TIMESTAMP ERROR - ${timestampValidationResp.message}")
						return timestampValidationResp
					}
				} else {
					String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
					log.error("ERROR - validateSignersCertificate - ${msg}")
					return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST)
				}
				checkedSigners.add(responseVS.userVS)
                if(userVS.getNif().equals(signerNIF)) checkedSigner = responseVS.userVS;
			} catch (CertPathValidatorException ex) {
				log.error(ex.getMessage(), ex)
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
					messageSource.getMessage('unknownCAErrorMSg', null, locale))
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new ResponseVS(message:ex.getMessage(), statusCode:ResponseVS.SC_ERROR_REQUEST)
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:messageWrapper,
                data:[checkedSigners:checkedSigners, checkedSigner:checkedSigner])
	} 
		        
			
	public PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors, 
		boolean checkCRL, List<X509Certificate> certs) throws Exception {
		PKIXParameters pkixParameters = new PKIXParameters(anchors);
		
		SVCertExtensionChecker checker = new SVCertExtensionChecker();
		pkixParameters.addCertPathChecker(checker);
		
		pkixParameters.setRevocationEnabled(checkCRL); // if false tell system do not check CRL's
		CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX","BC");
		CertificateFactory certFact = CertificateFactory.getInstance("X.509");
		CertPath certPath = certFact.generateCertPath(certs);
		CertPathValidatorResult result = certPathValidator.validate(certPath, pkixParameters);
		return (PKIXCertPathValidatorResult)result;
	}

	public ResponseVS validateSMIMEVote(
		SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVote -")
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVote - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateVoteCerts(messageWrapper, locale)
	}
		
	public ResponseVS validateVoteCerts(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		Set<UserVS> signersVS = smimeMessageReq.getSigners();
		String msg
		ResponseVS responseVS
		EventVSElection eventVS
		if(signersVS.isEmpty()) {
			msg = messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale)
			log.error ("validateVoteCerts - ERROR SIGNERS - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
		}
		VoteVS voteVS = smimeMessageReq.voteVS
		String localServerURL = grailsApplication.config.grails.serverURL
		String voteAccessControlURL = StringUtils.checkURL(voteVS.accessControlURL)
		if (!localServerURL.equals(voteAccessControlURL)) {
			msg = messageSource.getMessage('certVoteValidationErrorMsg',
                    [voteAccessControlURL, localServerURL].toArray(), locale)
			log.error ("validateVoteCerts - ERROR SERVER URL - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,type:TypeVS.VOTE_ERROR)
		}
		eventVS = EventVSElection.get(Long.valueOf(voteVS.getEventVS().getId()))
		if (!eventVS)  {
			msg = messageSource.getMessage('electionNotFound', [voteVS.getEventVS().getId()].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT NOT FOUND - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
		}
		if(eventVS.state != EventVS.State.ACTIVE) {
			msg = messageSource.getMessage('electionClosed', [eventVS.subject].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT '${eventVS.id}' STATE -> ${eventVS.state}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
		}
		CertificateVS certificate = CertificateVS.findWhere(hashCertVoteBase64:voteVS.hashCertVoteBase64,
			    state:CertificateVS.State.OK)
		if (!certificate) {
			msg = messageSource.getMessage('hashVoteValidationErrorMsg', [voteVS.hashCertVoteBase64].toArray(), locale)
			log.error ("validateVoteCerts - ERROR CERT '${msg}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
		}
		smimeMessageReq.voteVS.setCertificateVS(certificate)
		responseVS = getEventTrustedAnchors(eventVS, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
		Set<TrustAnchor> trustedAnchors = (Set<TrustAnchor>) responseVS.data
		//Vote validation
		PKIXCertPathValidatorResult pkixResult;
		X509Certificate certCaResult;
		X509Certificate checkedCert = voteVS.getX509Certificate()
		try {
			pkixResult = verifyCertificate(trustedAnchors, false, [checkedCert])
			certCaResult = pkixResult.getTrustAnchor().getTrustedCert();
			log.debug("validateVoteCerts - vote cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString()+
					"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('certValidationErrorMsg',
					[checkedCert.getSubjectDN()?.toString()].toArray(), locale)
			log.error ("validateVoteCerts - ERROR VOTE CERT VALIDATION -> '${msg}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
		}
        Date signatureTime = voteVS.getTimeStampToken()?.getTimeStampInfo().getGenTime()
        if(!eventVS.isActive(signatureTime)) {
            msg = messageSource.getMessage("checkedDateRangeErrorMsg", [signatureTime,
                    eventVS.getDateBegin(), eventVS.getDateFinish()].toArray(), locale)
            log.error(msg)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VOTE_ERROR,
                    message:msg, eventVS:eventVS)
        }
		//Control Center cert validation
		trustedAnchors = controlCenterTrustedAnchorsHashMap.get(eventVS?.id)
		if(!trustedAnchors) {
			trustedAnchors = new HashSet<TrustAnchor>()
			Collection<X509Certificate> controlCenterCerts = CertUtil.
                    fromPEMToX509CertCollection (eventVS.certChainControlCenter)
			for(X509Certificate controlCenterCert : controlCenterCerts)	{
				TrustAnchor anchor = new TrustAnchor(controlCenterCert, null);
				trustedAnchors.add(anchor);
			}
			controlCenterTrustedAnchorsHashMap.put(eventVS.id, trustedAnchors)
		}
		//check control center certificate
		if(voteVS.getServerCerts().isEmpty()) {
			msg = messageSource.getMessage('controlCenterMissingSignatureErrorMsg', null, locale)
			log.error(" ERROR - MISSING CONTROL CENTER SIGNATURE - msg: ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
		}
		checkedCert = voteVS.getServerCerts()?.iterator()?.next()
		try {
			pkixResult = verifyCertificate(trustedAnchors, false, [checkedCert])
			certCaResult = pkixResult.getTrustAnchor().getTrustedCert();
			log.debug("validateVoteCerts - Control Center cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString() +
					"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('certValidationErrorMsg',
					[checkedCert.getSubjectDN()?.toString()].toArray(), locale)
			log.error ("validateVoteCerts - ERROR CONTROL CENTER CERT VALIDATION -> '${msg}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
			smimeMessage:smimeMessageReq, type:TypeVS.CONTROL_CENTER_VALIDATED_VOTEVS)
	}
	
	private SignedMailGenerator getSignedMailGenerator() {
		if(signedMailGenerator == null) signedMailGenerator = afterPropertiesSet()
		return signedMailGenerator
	}

}