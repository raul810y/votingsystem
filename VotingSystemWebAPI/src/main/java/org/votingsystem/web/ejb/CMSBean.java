package org.votingsystem.web.ejb;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.cms.CMSGenerator;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.UserCertificationRequestDto;
import org.votingsystem.dto.voting.KeyStoreDto;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.*;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class CMSBean {

    private static Logger log = Logger.getLogger(CMSBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TimeStampBean timeStampBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    private CMSGenerator cmsGenerator;
    private Encryptor encryptor;
    private Set<TrustAnchor> trustAnchors;
    private Set<TrustAnchor> currencyAnchors;
    private Set<Long> anonymousCertIssuers;
    private Set<X509Certificate> trustedCerts;
    private PrivateKey serverPrivateKey;
    private CertificateVS serverCertificateVS;
    private X509Certificate localServerCertSigner;
    private List<X509Certificate> certChain;
    private byte[] keyStorePEMCerts;
    private Map<Long, CertificateVS> trustedCertsHashMap = new HashMap<>();
    private static final HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap = new HashMap<>();
    private Set<String> admins;
    private UserVS systemUser;
    private String password;
    private String keyAlias;

    public void init() throws Exception {
        Properties properties = new Properties();
        URL res = Thread.currentThread().getContextClassLoader().getResource("KeyStore.properties");
        log.info("init - res: " + res.toURI());
        properties.load(res.openStream());
        keyAlias = properties.getProperty("vs.signKeyAlias");
        password = properties.getProperty("vs.signKeyPassword");
        String keyStoreFileName = properties.getProperty("vs.keyStoreFile");
        res = Thread.currentThread().getContextClassLoader().getResource(keyStoreFileName);
        File keyStoreFile = FileUtils.getFileFromBytes(IOUtils.toByteArray(res.openStream()));
        cmsGenerator = new CMSGenerator(FileUtils.getBytesFromFile(keyStoreFile),
                keyAlias, password.toCharArray(), ContextVS.SIGNATURE_ALGORITHM);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        certChain = new ArrayList<>();
        for(java.security.cert.Certificate certificate : keyStore.getCertificateChain(keyAlias)) {
            checkAuthorityCertDB((X509Certificate) certificate);
            certChain.add((X509Certificate) certificate);
        }
        keyStorePEMCerts = PEMUtils.getPEMEncoded (certChain);
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        currencyAnchors = new HashSet<>();

        currencyAnchors.add(new TrustAnchor(localServerCertSigner, null));
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber")
                .setParameter("serialNumber", localServerCertSigner.getSerialNumber().longValue());
        serverCertificateVS = dao.getSingleResult(CertificateVS.class, query);
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
    }

    public boolean isAdmin(String nif) {
        return admins.contains(nif);
    }

    public void setAdmins(Set<String> admins) {
        this.admins = admins;
    }

    public UserVS getSystemUser() {
        return systemUser;
    }

    public byte[] getKeyStorePEMCerts() {
        return keyStorePEMCerts;
    }

    public Set<TrustAnchor> getCurrencyAnchors() {
        return currencyAnchors;
    }

    public KeyStoreInfo getKeyStoreInfo(byte[] keyStoreBytes, String keyAlias) throws Exception {
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password.toCharArray());
        PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        return new KeyStoreInfo(keyStore, privateKeySigner, certSigner);
    }

    public void initAdmins(List<UserVS> admins) throws Exception {
        systemUser = config.getSystemUser();
        Set<String> adminsNIF = new HashSet<>();
        for(UserVS userVS:admins) {
            verifyUserCertificate(userVS);
            userVS = subscriptionVSBean.checkUser(userVS);
            adminsNIF.add(userVS.getNif());
        }
        systemUser.updateAdmins(adminsNIF);
        dao.merge(systemUser);
        log.info("initAdmins - admins list:" + adminsNIF);
        setAdmins(adminsNIF);
    }

    public void initAnonymousCertAuthorities(List<X509Certificate>  anonymous_provider_TrustedCerts) throws Exception {
        anonymousCertIssuers = new HashSet<>();
        for(X509Certificate anonymous_provider:anonymous_provider_TrustedCerts) {
            anonymousCertIssuers.add(anonymous_provider.getSerialNumber().longValue());
        }
    }

    public void initCertAuthorities(List<X509Certificate> resourceCerts) throws Exception {
        log.info("initCertAuthorities - resourceCerts.size: " + resourceCerts.size());
        for(X509Certificate fileSystemX509TrustedCert:resourceCerts) {
            checkAuthorityCertDB(fileSystemX509TrustedCert);
        }
        Query query = dao.getEM().createQuery("SELECT c FROM CertificateVS c WHERE c.type in :typeList and c.state =:state")
                .setParameter("typeList", Arrays.asList(CertificateVS.Type.CERTIFICATE_AUTHORITY,
                        CertificateVS.Type.CERTIFICATE_AUTHORITY_ID_CARD))
                .setParameter("state", CertificateVS.State.OK);
        List<CertificateVS>  trustedCertsList = query.getResultList();
        trustedCertsHashMap = new HashMap<>();
        trustedCerts = new HashSet<>();
        trustAnchors = new HashSet<>();
        for (CertificateVS certificateVS : trustedCertsList) {
            addCertAuthority(certificateVS);
        }
    }

    public void addCertAuthority(CertificateVS certificateVS) throws Exception {
        X509Certificate x509Cert = certificateVS.getX509Cert();
        trustedCerts.add(x509Cert);
        trustedCertsHashMap.put(x509Cert.getSerialNumber().longValue(), certificateVS);
        TrustAnchor trustAnchor = new TrustAnchor(x509Cert, null);
        trustAnchors.add(trustAnchor);
        log.info("addCertAuthority - certificateVS.id: " + certificateVS.getId() + " - " + x509Cert.getSubjectDN() +
                " - num. trustedCerts: " + trustedCerts.size());
    }

    private CertificateVS checkAuthorityCertDB(X509Certificate x509AuthorityCert) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException, ExceptionVS {
        log.info(x509AuthorityCert.getSubjectDN().toString());
        Query query = dao.getEM().createQuery("SELECT c FROM CertificateVS c WHERE c.serialNumber =:serialNumber")
                .setParameter("serialNumber", x509AuthorityCert.getSerialNumber().longValue());
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null) {
            certificateVS = dao.persist(CertificateVS.AUTHORITY(x509AuthorityCert, null));
            log.info("ADDED NEW FILE SYSTEM CA CERT - certificateVS.id:" + certificateVS.getId() + " - type: " +
                    certificateVS.getType());
        } else if (CertificateVS.State.OK != certificateVS.getState()) {
            throw new ExceptionVS("File system athority cert: " + x509AuthorityCert.getSubjectDN() + " }' " +
                    " - certificateVS.id: " + certificateVS.getId() + " - state:" + certificateVS.getState());
        }
        return certificateVS;
    }

    public void validateVoteCerts(CMSSignedMessage cmsMessage, EventVS eventVS) throws Exception {
        Set<UserVS> signersVS = cmsMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS("ERROR - document without signers");
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS);
        for(UserVS userVS: signersVS) {
            CertUtils.verifyCertificate(eventTrustedAnchors, false, Arrays.asList(userVS.getCertificate()));
            //X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        }
    }

    public Set<TrustAnchor> getEventTrustedAnchors(EventVS eventVS) throws Exception {
        Set<TrustAnchor> eventTrustedAnchors = eventTrustedAnchorsMap.get(eventVS.getId());
        if(eventTrustedAnchors == null) {
            CertificateVS eventCACert = eventVS.getCertificateVS();
            X509Certificate certCAEventVS = eventCACert.getX509Cert();
            eventTrustedAnchors = new HashSet<>();
            eventTrustedAnchors.add(new TrustAnchor(certCAEventVS, null));
            eventTrustedAnchors.addAll(getTrustAnchors());
            eventTrustedAnchorsMap.put(eventVS.getId(), eventTrustedAnchors);
        }
        return eventTrustedAnchors;
    }

    public boolean isSignerCertificate(Set<UserVS> signers, X509Certificate cert) throws CertificateEncodingException {
        for(UserVS userVS : signers) {
            if(Arrays.equals(userVS.getCertificate().getEncoded(), cert.getEncoded())) return true;
        }
        return false;
    }

    public KeyStoreDto generateElectionKeysStore(EventVS eventVS) throws Exception {
        //StringUtils.getRandomAlphaNumeric(7).toUpperCase()
        // _ TODO _ ====== crypto token
        String eventVSUrl = config.getContextURL() + "/rest/eventElection/id/" + eventVS.getId();
        String strSubjectDNRoot = format("CN=eventVSUrl:{0}, OU=Elections", eventVSUrl);
        KeyStore keyStore = KeyStoreUtil.createRootKeyStore(eventVS.getDateBegin(), eventVS.getDateFinish(),
                password.toCharArray(), keyAlias, strSubjectDNRoot);
        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        java.security.cert.Certificate cert = chain[0];
        return new KeyStoreDto(new KeyStoreVS (keyAlias, KeyStoreUtil.getBytes(keyStore, password.toCharArray()),
                eventVS.getDateBegin(), eventVS.getDateFinish()), (X509Certificate) cert);
    }

    public KeyStore generateKeysStore(String givenName, String surname, String nif, char[] password) throws Exception {
        log.info("generateKeysStore - nif: " + nif);
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(localServerCertSigner,
                serverPrivateKey, keyAlias);
        String testUserDN = null;
        if(surname == null) testUserDN = format("GIVENNAME={0}, SERIALNUMBER={1}", givenName, nif);
        else testUserDN = format("GIVENNAME={0}, SURNAME={1} , SERIALNUMBER={2}", givenName, surname, nif);
        //String strSubjectDN = "CN=Voting System Cert Authority , OU=VotingSystem"
        //KeyStore rootCAKeyStore = KeyStoreUtil.createRootKeyStore (validFrom.getTime(), (validTo.getTime() - validFrom.getTime()),
        //        userPassword.toCharArray(), keyAlias, strSubjectDN);
        //X509Certificate certSigner = (X509Certificate)rootCAKeyStore.getCertificate(keyAlias);
        //PrivateKey privateKeySigner = (PrivateKey)rootCAKeyStore.getKey(keyAlias, userPassword.toCharArray());
        //X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKeySigner,  keyAlias);
        return KeyStoreUtil.createUserKeyStore(validFrom.getTime(),
                (validTo.getTime() - validFrom.getTime()), password, ContextVS.KEYSTORE_USER_CERT_ALIAS,
                rootCAPrivateCredential, testUserDN);
    }

    public X509Certificate getServerCert() {
        return localServerCertSigner;
    }

    public CertificateVS getServerCertificateVS() {
        return serverCertificateVS;
    }

    private PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    private Map<Long, CertificateVS> getTrustedCertsHashMap() {
        return trustedCertsHashMap;
    }

    public PKIXCertPathValidatorResult verifyCertificate(X509Certificate certToValidate) throws Exception {
        return CertUtils.verifyCertificate(getTrustAnchors(), false, Arrays.asList(certToValidate));
    }

    public boolean isSystemSignedMessage(Set<UserVS> signers) {
        for(UserVS userVS: signers) {
            if(userVS.getCertificate().equals(localServerCertSigner)) return true;
        }
        return false;
    }

    public X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, Date dateBegin,
                                   Date dateFinish) throws Exception {
        X509Certificate issuedCert = CertUtils.signCSR(csr, organizationalUnit, getServerPrivateKey(),
                getServerCert(), dateBegin, dateFinish);
        return issuedCert;
    }

    public CMSSignedMessage signData(String textToSign) throws Exception {
        return cmsGenerator.signData(textToSign);
    }

    public CMSSignedMessage signDataWithTimeStamp(String textToSign) throws Exception {
        CMSSignedMessage cmsMessage = cmsGenerator.signData(textToSign);
        return timeStampBean.timeStampCMS(cmsMessage);
    }

    public synchronized CMSSignedMessage addSignature (final CMSSignedMessage cmsMessage) throws Exception {
        return new CMSSignedMessage(cmsGenerator.addSignature(cmsMessage));
    }

    public CMSDto validateCMS(CMSSignedMessage cmsSignedMessage, ContentTypeVS contenType) throws Exception {
        if (cmsSignedMessage.isValidSignature()) {
            MessagesVS messages = MessagesVS.getCurrentInstance();
            Query query = dao.getEM().createNamedQuery("findcmsMessageByBase64ContentDigest")
                    .setParameter("base64ContentDigest", cmsSignedMessage.getContentDigestStr());
            CMSMessage cmsMessage = dao.getSingleResult(CMSMessage.class, query);
            if(cmsMessage != null) throw new ExceptionVS(messages.get("cmsDigestRepeatedErrorMsg",
                    cmsSignedMessage.getContentDigestStr()));
            CMSDto cmsDto = validateSignersCerts(cmsSignedMessage);
            TypeVS typeVS = TypeVS.OK;
            if(ContentTypeVS.CURRENCY == contenType) typeVS = TypeVS.CURRENCY;
            cmsMessage = dao.persist(new CMSMessage(cmsSignedMessage, cmsDto, typeVS));
            CMSMessage.setCurrentInstance(cmsMessage);
            cmsDto.setCmsMessage(cmsMessage);
            return cmsDto;
        } else throw new ValidationExceptionVS("invalid CMSMessage");
    }

    public CMSDto validatedVote(CMSSignedMessage cmsSignedMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findcmsMessageByBase64ContentDigest")
                .setParameter("base64ContentDigest", cmsSignedMessage.getContentDigestStr());
        CMSMessage cmsMessage = dao.getSingleResult(CMSMessage.class, query);
        if(cmsMessage != null) throw new ExceptionVS(messages.get("cmsDigestRepeatedErrorMsg",
                    cmsSignedMessage.getContentDigestStr()));
        Vote vote = cmsSignedMessage.getVote();
        CMSDto cmsDto = new CMSDto(vote);
        if(vote == null || vote.getX509Certificate() == null) throw new ExceptionVS(
                messages.get("documentWithoutSignersErrorMsg"));
        if (vote.getRepresentativeURL() != null) {
            query = dao.getEM().createQuery("select u from UserVS u where u.url =:userURL")
                    .setParameter("userURL", vote.getRepresentativeURL());
            UserVS checkedSigner = dao.getSingleResult(UserVS.class, query);
            if(checkedSigner == null) checkedSigner = dao.persist(UserVS.REPRESENTATIVE(vote.getRepresentativeURL()));
            cmsDto.setSigner(checkedSigner);
        }
        query = dao.getEM().createQuery("select e from EventVS e where e.accessControlEventId =:eventId and " +
                "e.accessControlVS.serverURL =:serverURL").setParameter("eventId", vote.getAccessControlEventId())
                .setParameter("serverURL", vote.getAccessControlURL());
        EventElection eventVS = dao.getSingleResult(EventElection.class, query);
        if(eventVS == null) throw new ExceptionVS(messages.get("voteEventElectionUnknownErrorMsg",
                vote.getAccessControlURL(), vote.getAccessControlEventId()));
        if(eventVS.getState() != EventVS.State.ACTIVE)
            throw new ExceptionVS(messages.get("electionClosed", eventVS.getSubject()));
        cmsDto.setEventVS(eventVS);
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS);
        timeStampBean.validateToken(vote.getTimeStampToken());
        X509Certificate checkedCert = vote.getX509Certificate();
        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                eventTrustedAnchors, false, Arrays.asList(checkedCert));
        validatorResult.getTrustAnchor().getTrustedCert();
        cmsDto.setCmsMessage(dao.persist(new CMSMessage(cmsSignedMessage, cmsDto, TypeVS.SEND_VOTE)));
        return cmsDto;
    }

    public CMSDto validatedVoteFromControlCenter(CMSSignedMessage cmsSignedMessage) throws Exception {
        CMSDto cmsDto = validatedVote(cmsSignedMessage);
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVS and c.state =:state")
                .setParameter("hashCertVS", cmsDto.getVote().getHashCertVSBase64()).setParameter("state", CertificateVS.State.OK);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if (certificateVS == null) {
            cmsDto.getCmsMessage().setType(TypeVS.ERROR).setReason("missing Vote CertificateVS");
            dao.merge(cmsDto.getCmsMessage());
            throw new ValidationExceptionVS("missing Vote CertificateVS");
        }
        cmsDto.getCmsMessage().getCMS().getVote().setCertificateVS(certificateVS);
        return cmsDto;
    }
    
    public PKIXCertPathValidatorResult validateCertificates(List<X509Certificate> certificateList) throws ExceptionVS {
        log.log(Level.FINE, "validateCertificates");
        return CertUtils.verifyCertificate(getTrustAnchors(), false, certificateList);
        //X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
    }

    public CMSDto validateSignersCerts(CMSSignedMessage cmsSignedMessage) throws Exception {
        Set<UserVS> signersVS = cmsSignedMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS("documentWithoutSignersErrorMsg");
        String signerNIF = null;
        if(cmsSignedMessage.getSigner().getNif() != null) signerNIF =
                org.votingsystem.util.NifUtils.validate(cmsSignedMessage.getSigner().getNif());
        CMSDto cmsDto = new CMSDto();
        for(UserVS userVS: signersVS) {
            if(userVS.getTimeStampToken() != null) timeStampBean.validateToken(userVS.getTimeStampToken());
            else log.info("signature without timestamp - signer: " + userVS.getCertificate().getSubjectDN());
            verifyUserCertificate(userVS);
            if(userVS.isAnonymousUser()) {
                log.log(Level.FINE, "validateSignersCerts - is anonymous signer");
                cmsDto.setAnonymousSigner(userVS);
            } else {
                UserVS user = subscriptionVSBean.checkUser(userVS);
                if(user.getNif().equals(signerNIF)) cmsDto.setSigner(user);
                else cmsDto.addSigner(user);
            }
        }
        return cmsDto;
    }

    public PKIXCertPathValidatorResult verifyUserCertificate(UserVS userVS) throws Exception {
        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                getTrustAnchors(), false, Arrays.asList(userVS.getCertificate()));
        X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        userVS.setCertificateCA(getTrustedCertsHashMap().get(certCaResult.getSerialNumber().longValue()));
        if(anonymousCertIssuers.contains(certCaResult.getSerialNumber().longValue()) &&
                Boolean.valueOf(CertUtils.getCertExtensionData(userVS.getCertificate(), ContextVS.ANONYMOUS_CERT_OID))) {
            userVS.setAnonymousUser(true);
        }
        log.log(Level.FINE, "verifyCertificate - user:" + userVS.getNif() + " cert issuer: " + certCaResult.getSubjectDN() +
                " - CA certificateVS.id : " + userVS.getCertificateCA().getId());
        return validatorResult;
    }

    //issues certificates if the request is signed with an Id card
    public X509Certificate signCSRSignedWithIDCard(CMSMessage cmsReq) throws Exception {
        UserVS signer = cmsReq.getUserVS();
        if(signer.getCertificateVS().getType() != CertificateVS.Type.USER_ID_CARD)
                throw new Exception("Service available only for ID CARD signed requests");
        UserCertificationRequestDto requestDto = cmsReq.getSignedContent(UserCertificationRequestDto.class);
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(requestDto.getCsrRequest());
        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                CertExtensionDto.class, csr, ContextVS.DEVICEVS_OID);
        String validatedNif = NifUtils.validate(certExtensionDto.getNif());
        AddressVS address = signer.getAddressVS();
        if(address == null) {
            address = dao.persist(requestDto.getAddressVS());
            signer.setAddressVS(address);
            dao.merge(signer);
        } else {
            address.update(requestDto.getAddressVS());
            dao.merge(address);
        }
        dao.merge(signer);
        Date validFrom = new Date();
        Date validTo = DateUtils.addDays(validFrom, 365).getTime(); //one year
        X509Certificate issuedCert = signCSR(csr, null, validFrom, validTo);
        CertificateVS certificate = dao.persist(CertificateVS.ISSUED_USER_CERT(signer, issuedCert, serverCertificateVS));
        Query query = dao.getEM().createQuery("select d from DeviceVS d where d.deviceId =:deviceId and d.userVS.nif =:nif ")
                .setParameter("deviceId", certExtensionDto.getDeviceId()).setParameter("nif", validatedNif);
        DeviceVS deviceVS = dao.getSingleResult(DeviceVS.class, query);
        if(deviceVS == null) {
            deviceVS = dao.persist(new DeviceVS(signer, certExtensionDto.getDeviceId(), certExtensionDto.getEmail(),
                    certExtensionDto.getMobilePhone(), certExtensionDto.getDeviceType()).setState(DeviceVS.State.OK)
                    .setCertificateVS(certificate));
        } else {
            dao.merge(deviceVS.getCertificateVS().setState(CertificateVS.State.CANCELED).setCmsMessage(cmsReq));
            dao.merge(deviceVS.setEmail(certExtensionDto.getEmail()).setPhone(certExtensionDto.getMobilePhone())
                    .setCertificateVS(certificate));
        }
        dao.getEM().createQuery("UPDATE UserVSToken SET state=:state WHERE userVS=:userVS").setParameter(
                "state", UserVSToken.State.CANCELLED).setParameter("userVS", signer).executeUpdate();
        dao.persist(new UserVSToken(signer, requestDto.getToken(), certificate, cmsReq));
        log.info("signCertUserVS - issued new CertificateVS id: " + certificate.getId() + " for device: " + deviceVS.getId());
        return issuedCert;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        return trustAnchors;
    }

    public Set<X509Certificate> getTrustedCerts() {
        return trustedCerts;
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return encryptor.encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey publicKey) throws Exception {
        return encryptor.encryptToCMS(dataToEncrypt, publicKey);
    }

    public byte[] decryptCMS (byte[] encryptedMessageBytes) throws Exception {
        return encryptor.decryptCMS(encryptedMessageBytes);
    }

    public List<X509Certificate> getCertChain() {
        return certChain;
    }

}