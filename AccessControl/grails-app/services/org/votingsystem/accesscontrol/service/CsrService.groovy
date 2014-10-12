package org.votingsystem.accesscontrol.service

import grails.converters.JSON
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.signature.util.KeyStoreUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate

class CsrService {

    LinkGenerator grailsLinkGenerator
	def grailsApplication
	def subscriptionVSService
	def messageSource
    def signatureVSService
	
	public synchronized ResponseVS signCertVoteVS (byte[] csrPEMBytes, EventVS eventVS, UserVS userVS, Locale locale) {
		log.debug("signCertVoteVS - eventVS: ${eventVS?.id}");
		ResponseVS responseVS = validateCSRVote(csrPEMBytes, eventVS, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
		PublicKey requestPublicKey = responseVS.data.publicKey
		KeyStoreVS keyStoreVS = eventVS.getKeyStoreVS()
		//TODO ==== vote keystore -- this is for developement
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreVS.bytes,
			grailsApplication.config.VotingSystem.signKeysPassword.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyStoreVS.keyAlias,
			grailsApplication.config.VotingSystem.signKeysPassword.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyStoreVS.keyAlias);
        DERTaggedObject representativeExtension = null
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		if(userVS?.type == UserVS.Type.REPRESENTATIVE) {
            String representativeURL = "${grailsLinkGenerator.link(controller:"representative", absolute:true)}/${userVS?.id}"
            representativeExtension = new DERTaggedObject(ContextVS.REPRESENTATIVE_VOTE_TAG,
                    new DERUTF8String(representativeURL))
        }

		X509Certificate issuedCert = CertUtils.signCSR(csr, null, privateKeySigner, certSigner, eventVS.dateBegin,
                eventVS.dateFinish, representativeExtension)
		if (!issuedCert) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error(msg)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
		} else {
			CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
				    content:issuedCert.getEncoded(), eventVSElection:eventVS,
                    type:CertificateVS.Type.VOTEVS, state:CertificateVS.State.OK,
				    hashCertVSBase64:responseVS.data.hashCertVSBase64)
            if(userVS?.type == UserVS.Type.REPRESENTATIVE) certificate.setUserVS(userVS)
			certificate.save()
			byte[] issuedCertPEMBytes = CertUtils.getPEMEncoded(issuedCert);
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
                    data:[requestPublicKey:requestPublicKey, issuedCert:issuedCertPEMBytes])
		}
	}

    private ResponseVS validateCSRVote(byte[] csrPEMBytes, EventVSElection eventVS, Locale locale) {
        PKCS10CertificationRequest csr
        try {
            csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        } catch(ex) {
            log.error(ex.getMessage(), ex)
            throw new ExceptionVS(messageSource.getMessage('csrRequestErrorMsg', null, locale))
        }
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        def voteCertDataJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.VOTE_TAG:
                    voteCertDataJSON = JSON.parse(((DERUTF8String)attribute.getObject()).getString())
                    break;
            }
        }
        String eventId = voteCertDataJSON.eventId
        log.debug("validateCSRVote - accessControlURL: ${voteCertDataJSON.accessControlURL} - " +
                "eventId: ${voteCertDataJSON.eventId} - hashCertVS: ${voteCertDataJSON.hashCertVS}")
        if (!eventId.equals(String.valueOf(eventVS.getId()))) {
            String msg = messageSource.getMessage('csrRequestError', null, locale)
            log.error("- validateCSRVote - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
        }
        String accessControlURL = StringUtils.checkURL(voteCertDataJSON.accessControlURL)
        String serverURL = grailsApplication.config.grails.serverURL
        if (!serverURL.equals(accessControlURL)) {
            String msg = messageSource.getMessage('accessControlURLError',[serverURL,accessControlURL].toArray(),locale)
            log.error("- validateCSRVote - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ACCESS_REQUEST_ERROR)
        }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ACCESS_REQUEST,
                data:[publicKey:csr.getPublicKey(), hashCertVSBase64:voteCertDataJSON.hashCertVS])
    }

    public synchronized ResponseVS signAnonymousDelegationCert (byte[] csrPEMBytes, Locale locale) {
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        if(!csr) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signAnonymousDelegationCert - msg:  ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        Enumeration csrAttributes = info.getAttributes().getObjects()
        def certAttributeJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG:
                    String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString()
                    certAttributeJSON = JSON.parse(certAttributeJSONStr)
                    break;
            }
        }
        if(!certAttributeJSON) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signAnonymousDelegationCert - missing certAttributeJSON")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        String serverURL = grailsApplication.config.grails.serverURL
        String accessControlURL = StringUtils.checkURL(certAttributeJSON.accessControlURL)
        if (!serverURL.equals(accessControlURL) || !certAttributeJSON.hashCertVS ) {
            String msg = messageSource.getMessage('accessControlURLError',[serverURL,accessControlURL].toArray(),locale)
            log.error("- signAnonymousDelegationCert - ERROR - ${msg}")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.ERROR)
        }
        //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
        String hashCertVSBase64 = certAttributeJSON.hashCertVS
        Date certValidFrom = DateUtils.getMonday(Calendar.getInstance()).getTime()
        Calendar calendarValidTo = Calendar.getInstance();
        calendarValidTo.add(Calendar.DATE, ContextVS.DAYS_ANONYMOUS_DELEGATION_DURATION);
        X509Certificate issuedCert = signatureVSService.signCSR(csr, null, certValidFrom, calendarValidTo.getTime())
        if (!issuedCert) {
            String msg = messageSource.getMessage('csrRequestErrorMsg', null, locale)
            log.error("signAnonymousDelegationCert - error signing cert")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg)
        } else {
            CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber().longValue(),
                    content:issuedCert.getEncoded(), type:CertificateVS.Type.ANONYMOUS_REPRESENTATIVE_DELEGATION,
                    state:CertificateVS.State.OK, hashCertVSBase64:hashCertVSBase64, validFrom:certValidFrom,
                    validTo: calendarValidTo.getTime())
            certificate.save()
            log.debug("signAnonymousDelegationCert - expended CertificateVS '${certificate.id}'")
            byte[] issuedCertPEMBytes = CertUtils.getPEMEncoded(issuedCert);
            Map data = [requestPublicKey:csr.getPublicKey()]
            return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST,
                    data:data, message:"certificateVS_${certificate.id}" , messageBytes:issuedCertPEMBytes)
        }
    }

	public X509Certificate getVoteCert(byte[] pemCertCollection) throws Exception {
		Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(pemCertCollection);
		for (X509Certificate certificate : certificates) {
			if (certificate.subjectDN.toString().contains("SERIALNUMBER=hashCertVoteHex:")) {
				return certificate
			}
		}
		return null
	}

    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
        emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
	public ResponseVS saveUserCSR(byte[] csrPEMBytes, Locale locale) {
		PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
		CertificationRequestInfo info = csr.getCertificationRequestInfo();
        String givenname;
        String surname;
        String nif;
		String email;
		String phone;
		String deviceId;
		String subjectDN = info.getSubject().toString();
		log.debug("saveUserCSR - subject: " + subjectDN)
        if(subjectDN.split("GIVENNAME=").length > 1)  givenname = subjectDN.split("GIVENNAME=")[1].split(",")[0]
        if(subjectDN.split("SURNAME=").length > 1)  surname = subjectDN.split("SURNAME=")[1].split(",")[0]
		if(subjectDN.split("SERIALNUMBER=").length > 1) {
			nif = subjectDN.split("SERIALNUMBER=")[1];
			if (nif.split(",").length > 1)  nif = nif.split(",")[0];
		}

        Enumeration csrAttributes = info.getAttributes().getObjects()
        def deviceDataJSON
        while(csrAttributes.hasMoreElements()) {
            DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
            switch(attribute.getTagNo()) {
                case ContextVS.DEVICEVS_TAG:
                    deviceDataJSON = JSON.parse(((DERUTF8String)attribute.getObject()).getString())
                    break;
            }
        }

		ResponseVS responseVS = subscriptionVSService.checkDevice(givenname, surname, nif, deviceDataJSON?.mobilePhone,
                deviceDataJSON?.email,  deviceDataJSON?.deviceId, locale)
		if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS;
		UserRequestCsrVS requestCSR
		def previousRequest = UserRequestCsrVS.findAllByDeviceVSAndUserVSAndState(
			responseVS.data, responseVS.userVS, UserRequestCsrVS.State.PENDING)
		previousRequest.each {eventVSItem ->
			eventVSItem.state = UserRequestCsrVS.State.CANCELLED
			eventVSItem.save();
		}
		UserRequestCsrVS.withTransaction {
			requestCSR = new UserRequestCsrVS(state:UserRequestCsrVS.State.PENDING,
				content:csrPEMBytes, userVS:responseVS.userVS,deviceVS:responseVS.data).save()
		}
		if(requestCSR) return new ResponseVS(statusCode:ResponseVS.SC_OK, message:requestCSR.id)
		else return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST)
	}
	
	public synchronized ResponseVS signCertUserVS (UserRequestCsrVS requestCSR, Locale locale) {
		log.debug("signCertUserVS");
		File keyStoreFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String keyAlias = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
		//log.debug("signCertUserVS - certSigner:${certSigner}");
		Date today = DateUtils.getMonday(Calendar.getInstance()).getTime();
		Calendar today_plus_year = Calendar.getInstance();
		today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(requestCSR.content);
        X509Certificate issuedCert = CertUtils.signCSR(csr, null, privateKeySigner,
                certSigner, today, today_plus_year.getTime())

		if (!issuedCert) {
			return new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    messageSource.getMessage("csrValidationErrorMsg", null, locale))
		} else {
			requestCSR.state = UserRequestCsrVS.State.OK
			requestCSR.serialNumber = issuedCert.getSerialNumber().longValue()
			requestCSR.save()
			CertificateVS certificate = new CertificateVS(serialNumber:issuedCert.getSerialNumber()?.longValue(),
				content:issuedCert.getEncoded(), userVS:requestCSR.userVS, state:CertificateVS.State.OK,
				userRequestCsrVS:requestCSR, type:CertificateVS.Type.USER)
			certificate.save()
			return new ResponseVS(statusCode:ResponseVS.SC_OK)
		}
	}

}
