package org.votingsystem.accesscontrol.service

import grails.transaction.Transactional
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.votingsystem.callable.MessageTimeStamper
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.Encryptor
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

import javax.mail.Header
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertPathValidatorException
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

@Transactional
class SignatureVSService {

    private SignedMailGenerator signedMailGenerator;
    private static Set<X509Certificate> trustedCerts;
    private static Set<TrustAnchor> trustAnchors;
    private KeyStore trustedCertsKeyStore
    static HashMap<Long, CertificateVS> trustedCertsHashMap;
    private X509Certificate localServerCertSigner;
    private PrivateKey serverPrivateKey;
    private Encryptor encryptor;
    private static HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap =  new HashMap<Long, Set<TrustAnchor>>();
    def grailsApplication;
    def messageSource
    def subscriptionVSService
    def timeStampService
    def sessionFactory

    public synchronized Map init() throws Exception {
        log.debug(" - init - ")
        File keyStoreFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.keyStorePath).getFile()
        String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
        String password = grailsApplication.config.VotingSystem.signKeysPassword
        signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStoreFile),
                keyAlias, password.toCharArray(), ContextVS.SIGN_MECHANISM);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        java.security.cert.Certificate[] localServerCertChain = keyStore.getCertificateChain(keyAlias);
        byte[] pemCertsArray
        for (int i = 0; i < localServerCertChain.length; i++) {
            checkAuthorityCertDB(localServerCertChain[i])
            if(!pemCertsArray) pemCertsArray = CertUtils.getPEMEncoded (localServerCertChain[i])
            else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtils.getPEMEncoded (localServerCertChain[i]))
        }
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray())
        File certChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        certChainFile.createNewFile()
        certChainFile.setBytes(pemCertsArray)
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
        initCertAuthorities();
        return [signedMailGenerator:signedMailGenerator, encryptor:encryptor, serverPrivateKey:serverPrivateKey,
                localServerCertSigner:localServerCertSigner];
    }

    public X509Certificate getServerCert() {
        if(localServerCertSigner == null) localServerCertSigner = init().localServerCertSigner
        return localServerCertSigner
    }

    private PrivateKey getServerPrivateKey() {
        if(serverPrivateKey == null) serverPrivateKey = init().serverPrivateKey
        return serverPrivateKey
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

    public ResponseVS getEventTrustedCerts(EventVS event, Locale locale) {
        log.debug("getEventTrustedCerts")
        if(!event) throw new ExceptionVS("EventVS null")
        CertificateVS eventVSCertificateVS = CertificateVS.findWhere(eventVSElection:event, state:CertificateVS.State.OK,
                type:CertificateVS.Type.VOTEVS_ROOT)
        if(!eventVSCertificateVS) {
            String msg = messageSource.getMessage('eventWithoutCAErrorMsg', [event.id].toArray(), locale)
            log.error ("getEventTrustedCerts - ERROR EVENT CA CERT -> '${msg}'")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:event)
        }
        X509Certificate certCAEventVS = CertUtils.loadCertificate(eventVSCertificateVS.content)
        Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>()
        eventTrustedCerts.add(certCAEventVS)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, data:eventTrustedCerts)
    }

    /**
     * Generate V3 Certificate from CSR
     */
    public X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, Date dateBegin,
                                   Date dateFinish, DERTaggedObject... certExtensions) throws Exception {
        X509Certificate issuedCert = CertUtils.signCSR(csr, organizationalUnit, getServerPrivateKey(),
                getServerCert(), dateBegin, dateFinish, certExtensions)
        return issuedCert
    }

    public Set<TrustAnchor> getEventTrustedAnchors(EventVS eventVS, Locale locale) {
        Set<TrustAnchor> eventTrustedAnchors = eventTrustedAnchorsMap.get(eventVS.id)
        if(!eventTrustedAnchors) {
            CertificateVS eventCACert = CertificateVS.findWhere(eventVSElection:eventVS, state:CertificateVS.State.OK,
                    type:CertificateVS.Type.VOTEVS_ROOT)
            X509Certificate certCAEventVS = eventCACert.getX509Cert()
            eventTrustedAnchors = new HashSet<TrustAnchor>()
            eventTrustedAnchors.add(new TrustAnchor(certCAEventVS, null))
            eventTrustedAnchors.addAll(getTrustAnchors())
            eventTrustedAnchorsMap.put(eventVS.id, eventTrustedAnchors)
        }
        return eventTrustedAnchors
    }

    public KeyStore getTrustedCertsKeyStore() {
        if(!trustedCertsKeyStore || trustedCertsKeyStore.size() != trustedCerts.size()) {
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

    @Transactional
    private CertificateVS checkAuthorityCertDB(X509Certificate x509AuthorityCert) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        String certData = "${x509AuthorityCert?.getSubjectDN()} - numSerie:${x509AuthorityCert?.getSerialNumber()?.longValue()}"
        CertificateVS certificateVS = CertificateVS.findWhere(serialNumber:x509AuthorityCert.getSerialNumber().longValue(),
                type:CertificateVS.Type.CERTIFICATE_AUTHORITY)
        if(!certificateVS) {
            certificateVS = new CertificateVS(isRoot:CertUtils.isSelfSigned(x509AuthorityCert),
                    type:CertificateVS.Type.CERTIFICATE_AUTHORITY, state:CertificateVS.State.OK,
                    content:x509AuthorityCert.getEncoded(),
                    serialNumber:x509AuthorityCert.getSerialNumber().longValue(),
                    validFrom:x509AuthorityCert.getNotBefore(),
                    validTo:x509AuthorityCert.getNotAfter()).save()
            log.debug "$methodName - ADDED NEW FILE SYSTEM CA CERT - $certData - certificateVS.id:'${certificateVS?.id}'"
        } else if (CertificateVS.State.OK != certificateVS.state) {
            throw new ExceptionVS("File system athority cert '${x509AuthorityCert?.getSubjectDN()}' " +
                            " - certificateVS.id:'${certificateVS?.id}' state: '${certificateVS.state}'")
        } else if(certificateVS.type != CertificateVS.Type.CERTIFICATE_AUTHORITY) {
            String msg = "Updated from type '${certificateVS.type}'  to type 'CERTIFICATE_AUTHORITY'"
            certificateVS.description = "${certificateVS.description} #### $msg"
            certificateVS.type = CertificateVS.Type.CERTIFICATE_AUTHORITY
            certificateVS.save()
            log.debug "$methodName - $certData - $msg"
        }
        return certificateVS
    }

    public synchronized Map initCertAuthorities() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
        File[] acFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("AC_") && fileName.endsWith(".pem");
            }
        });
        for(File caFile:acFiles) {
            X509Certificate fileSystemX509TrustedCert = CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromFile(caFile))
            checkAuthorityCertDB(fileSystemX509TrustedCert)
        }
        List<CertificateVS>  trustedCertsList = CertificateVS.createCriteria().list(offset: 0) {
            eq("state", CertificateVS.State.OK)
            eq("type",	CertificateVS.Type.CERTIFICATE_AUTHORITY)
        }
        trustedCertsHashMap = new HashMap<Long, CertificateVS>();
        trustedCerts = new HashSet<X509Certificate>();
        trustAnchors = new HashSet<TrustAnchor>();
        for (CertificateVS certificateVS : trustedCertsList) {
            X509Certificate x509Cert = certificateVS.getX509Cert()
            trustedCerts.add(x509Cert)
            trustedCertsHashMap.put(x509Cert.getSerialNumber()?.longValue(), certificateVS)
            trustAnchors.add(new TrustAnchor(x509Cert, null));
            String certData = "${x509Cert?.getSubjectDN()} - numSerie:${x509Cert?.getSerialNumber()?.longValue()}"
            log.debug "$methodName - certificateVS.id: '${certificateVS?.id}' - $certData"
        }
        log.debug("$methodName - loaded '${trustedCertsHashMap?.keySet().size()}' authorities")
        return [trustedCertsHashMap: trustedCertsHashMap, trustedCerts:trustedCerts, trustAnchors:trustAnchors]
    }

    public Set<X509Certificate> getTrustedCerts() {
        if(!trustedCerts || trustedCerts.isEmpty()) trustedCerts = initCertAuthorities().trustedCerts
        return trustedCerts;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        if(!trustAnchors || trustAnchors.isEmpty()) trustAnchors = initCertAuthorities().trustAnchors
        return trustAnchors;
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

    public SMIMEMessage getSMIMEMessage (String fromUser,String toUser,String textToSign,String subject, Header header) {
        log.debug "getSMIMEMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage mimeMessage = getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject, header)
        return mimeMessage;
    }

    public ResponseVS getTimestampedSignedMimeMessage (String fromUser,String toUser,String textToSign,String subject,
                                                       Header... headers) {
        log.debug "getTimestampedSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
        if(fromUser) fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        if(toUser) toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
        SMIMEMessage smimeMessage = getSignedMailGenerator().genMimeMessage(
                fromUser, toUser, textToSign, subject, headers)
        MessageTimeStamper timeStamper = new MessageTimeStamper(
                smimeMessage, "${grailsApplication.config.VotingSystem.urlTimeStampServer}/timeStamp")
        ResponseVS responseVS = timeStamper.call();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        smimeMessage = timeStamper.getSmimeMessage();
        responseVS = new ResponseVS(ResponseVS.SC_OK)
        responseVS.setSmimeMessage(smimeMessage)
        return responseVS;
    }

    public synchronized SMIMEMessage getMultiSignedMimeMessage (
            String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) {
        log.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
        if(fromUser) {
            fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
            smimeMessage.setFrom(new InternetAddress(fromUser))
        }
        if(toUser) {
            toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
            smimeMessage.setHeader("To", toUser)
        }
        SMIMEMessage multiSignedMessage = getSignedMailGenerator().genMultiSignedMessage(smimeMessage, subject);
        return multiSignedMessage
    }

    public CertificateVS getCACertificate(long numSerie) {
        log.debug("getCACertificate - numSerie: '${numSerie}'")
        return trustedCertsHashMap.get(numSerie)
    }

    public ResponseVS validateSMIME(SMIMEMessage smimeMessage, Locale locale) {
        MessageSMIME messageSMIME = null
        MessageSMIME.withTransaction {
            messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessage.getContentDigestStr())
        }
        if(messageSMIME) {
            String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
                    [smimeMessage.getContentDigestStr()].toArray(), locale)
            log.error("validateSMIME - ${message}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:message)
        }
        return validateSignersCerts(smimeMessage, locale)
    }

    public ResponseVS validateSignersCerts(SMIMEMessage smimeMessage, Locale locale) {
        Set<UserVS> signersVS = smimeMessage.getSigners();
        if(signersVS.isEmpty()) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale))
        Set<UserVS> checkedSigners = new HashSet<UserVS>()
        UserVS checkedSigner = null
        UserVS anonymousSigner = null
        CertUtils.CertValidatorResultVS validatorResult
        String signerNIF = smimeMessage.getSigner().getNif()
        for(UserVS userVS: signersVS) {
            try {
                if(userVS.getTimeStampToken() != null) {
                    ResponseVS timestampValidationResp = timeStampService.validateToken(
                            userVS.getTimeStampToken(), locale)
                    log.debug("validateSignersCerts - timestampValidationResp - " +
                            "statusCode:${timestampValidationResp.statusCode} - message:${timestampValidationResp.message}")
                    if(ResponseVS.SC_OK != timestampValidationResp.statusCode) {
                        log.error("validateSignersCerts - TIMESTAMP ERROR - ${timestampValidationResp.message}")
                        return timestampValidationResp
                    }
                } else if(smimeMessage.getTimeStampToken() == null) {
                    String msg = messageSource.getMessage('documentWithoutTimeStampErrorMsg', null, locale)
                    log.error("ERROR - validateSignersCerts - ${msg}")
                    return new ResponseVS(message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST)
                }
                validatorResult = CertUtils.verifyCertificate(getTrustAnchors(), false, [userVS.getCertificate()])
                X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
                userVS.setCertificateCA(trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue()))
                log.debug("validateSignersCerts - user cert issuer: " + certCaResult?.getSubjectDN()?.toString() +
                        " - issuer serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
                ResponseVS responseVS = null
                if(validatorResult.getChecker().isAnonymousSigner()) {
                    log.debug("validateSignersCerts - anonymous signer")
                    anonymousSigner = userVS
                    responseVS = new ResponseVS(ResponseVS.SC_OK)
                    responseVS.setUserVS(anonymousSigner)
                } else {
                    responseVS = subscriptionVSService.checkUser(userVS, locale)
                    if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
                    if(userVS.getNif().equals(signerNIF)) checkedSigner = responseVS.userVS;
                }
                checkedSigners.add(responseVS.userVS)
            } catch (CertPathValidatorException ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
                        messageSource.getMessage('unknownCAErrorMsg', null, locale))
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
                return new ResponseVS(message:ex.getMessage(), statusCode:ResponseVS.SC_ERROR)
            }
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, smimeMessage:smimeMessage,
                data:[checkedSigners:checkedSigners, checkedSigner:checkedSigner, anonymousSigner:anonymousSigner,
                      extensionChecker:validatorResult.getChecker()])
    }

    public ResponseVS validateSMIMEVote(SMIMEMessage smimeMessageReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
        if(messageSMIME) {
            String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
                    [smimeMessageReq.getContentDigestStr()].toArray(), locale)
            log.error("validateSMIMEVote - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
        }
        Set<UserVS> signersVS = smimeMessageReq.getSigners();
        String msg
        ResponseVS responseVS
        EventVSElection eventVS
        if(signersVS.isEmpty()) {
            msg = messageSource.getMessage('documentWithoutSignersErrorMsg', null, locale)
            log.error ("$methodName - ERROR SIGNERS - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
        }
        VoteVS voteVS = smimeMessageReq.voteVS
        String localServerURL = grailsApplication.config.grails.serverURL
        String voteAccessControlURL = StringUtils.checkURL(voteVS.accessControlURL)
        if (!localServerURL.equals(voteAccessControlURL)) {
            msg = messageSource.getMessage('certVoteValidationErrorMsg',
                    [voteAccessControlURL, localServerURL].toArray(), locale)
            log.error ("$methodName - ERROR SERVER URL - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,type:TypeVS.VOTE_ERROR)
        }
        eventVS = EventVSElection.get(Long.valueOf(voteVS.getEventVS().getId()))
        if (!eventVS)  {
            msg = messageSource.getMessage('electionNotFound', [voteVS.getEventVS().getId()].toArray(), locale)
            log.error ("$methodName - ERROR EVENT NOT FOUND - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VOTE_ERROR)
        }
        if(eventVS.state != EventVS.State.ACTIVE) {
            msg = messageSource.getMessage('electionClosed', [eventVS.subject].toArray(), locale)
            log.error ("$methodName - ERROR EVENT '${eventVS.id}' STATE -> ${eventVS.state}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        CertificateVS certificate = CertificateVS.findWhere(hashCertVSBase64:voteVS.hashCertVSBase64,
                state:CertificateVS.State.OK)
        if (!certificate) {
            msg = messageSource.getMessage('hashVoteValidationErrorMsg', [voteVS.hashCertVSBase64].toArray(), locale)
            log.error ("$methodName - ERROR CERT '${msg}'")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        smimeMessageReq.voteVS.setCertificateVS(certificate)
        Set<TrustAnchor> trustedAnchors = getEventTrustedAnchors(eventVS, locale)
        CertUtils.CertValidatorResultVS validatorResult;
        X509Certificate certCaResult;
        X509Certificate checkedCert = voteVS.getX509Certificate()
        try {
            validatorResult = CertUtils.verifyCertificate(trustedAnchors, false, [checkedCert])
            certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
            log.debug("$methodName - vote cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString()+
                    "- serialNumber: " + certCaResult?.getSerialNumber()?.longValue());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            msg = messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale)
            log.error ("$methodName - ERROR VOTE CERT VALIDATION -> '${msg}'")
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
        if(voteVS.getServerCerts().isEmpty()) {//check control center certificate
            msg = messageSource.getMessage('controlCenterMissingSignatureErrorMsg', null, locale)
            log.error(" ERROR - MISSING CONTROL CENTER SIGNATURE - msg: ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
                    type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        checkedCert = voteVS.getServerCerts()?.iterator()?.next()
        try {
            validatorResult = CertUtils.verifyCertificate(trustedAnchors, false, [checkedCert])
            certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
            log.debug("$methodName - Control Center cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString() +
                    "- numserie: " + certCaResult?.getSerialNumber()?.longValue());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex)
            msg = messageSource.getMessage('certValidationErrorMsg',
                    [checkedCert.getSubjectDN()?.toString()].toArray(), locale)
            log.error ("$methodName - ERROR CONTROL CENTER CERT VALIDATION -> '${msg}'")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message:msg, type:TypeVS.VOTE_ERROR, eventVS:eventVS)
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:eventVS,
                smimeMessage:smimeMessageReq, type:TypeVS.CONTROL_CENTER_VALIDATED_VOTE)
    }

    public ResponseVS encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        log.debug("encryptToCMS ${new String(dataToEncrypt)}")
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }

    public ResponseVS encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        log.debug("encryptMessage(...) - ");
        try {
            return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt files attached to SMIME (not signed) messages
     */
    public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
        log.debug "decryptMessage"
        try {
            return getEncryptor().decryptMessage(encryptedFile);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to encrypt SMIME signed messages
     */
    ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
        log.debug("encryptSMIMEMessage(...) ");
        try {
            return getEncryptor().encryptSMIMEMessage(bytesToEncrypt, receiverCert);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(messageSource.getMessage('dataToEncryptErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    /**
     * Method to decrypt SMIME signed messages
     */
    ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
        log.debug("decryptSMIMEMessage ")
        try {
            return getEncryptor().decryptSMIMEMessage(encryptedMessageBytes);
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            return new ResponseVS(message:messageSource.getMessage('encryptedMessageErrorMsg', null, locale),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    private Encryptor getEncryptor() {
        if(encryptor == null) encryptor = init().encryptor
        return encryptor;
    }

    private SignedMailGenerator getSignedMailGenerator() {
        if(signedMailGenerator == null) signedMailGenerator = init().signedMailGenerator
        return signedMailGenerator
    }

}