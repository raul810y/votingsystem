package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CMSUtils
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.*

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class VicketService {

    def messageSource
    def transactionVSService
    def grailsApplication
    def signatureVSService
    def userVSService
    def csrService
    def walletVSService
    def timeStampService
    def systemService

    public ResponseVS cancelVicket(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        UserVS signer = messageSMIMEReq.userVS
        def requestJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if(TypeVS.VICKET_CANCEL != TypeVS.valueOf(requestJSON.operation))
                throw new ExceptionVS(messageSource.getMessage("operationMismatchErrorMsg",
                [TypeVS.VICKET_CANCEL.toString(),requestJSON.operation ].toArray(), locale))
        def hashCertVSBase64 = CMSUtils.getHashBase64(requestJSON.originHashCertVS, ContextVS.VOTING_DATA_DIGEST)
        if(!hashCertVSBase64.equals(requestJSON.hashCertVSBase64))
            throw new ExceptionVS(messageSource.getMessage("originHashErrorMsg", null, locale))
        Vicket vicket = Vicket.findWhere(hashCertVS: requestJSON.hashCertVSBase64,
                serialNumber:Long.valueOf(requestJSON.vicketCertSerialNumber))
        if(Vicket.State.OK == vicket.getState()) {
            String fromUser = grailsApplication.config.VotingSystem.serverName
            String toUser = smimeMessageReq.getFrom().toString()
            String subject = messageSource.getMessage('cancelVicketReceiptSubject', null, locale)
            vicket.setState(Vicket.State.CANCELLED)
            SMIMEMessage receipt = signatureVSService.getSMIMEMultiSigned(fromUser, toUser,
                    smimeMessageReq, subject)
            messageSMIMEReq.setSMIME(receipt)
            vicket.cancelMessage = messageSMIMEReq
            vicket.save()
            TransactionVS transaction = new TransactionVS(amount: vicket.amount, messageSMIME:messageSMIMEReq,
                    subject:messageSource.getMessage('cancelVicketTransactionSubject', null, locale),
                    fromUserVS:signer, toUserVS:signer, state:TransactionVS.State.OK,
                    currency:vicket.currency, type:TransactionVS.Type.VICKET_CANCELLATION, validTo:vicket.validTo).save()
            log.debug("cancelVicket - model: ${vicket.id} - transactionVS: ${transaction.id}");
            return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_SIGNED,
                    messageBytes: vicket.cancelMessage.content, type:TypeVS.VICKET_CANCEL)
        } else {
            log.error("$methodName - ERROR - request for cancel vicket: ${vicket.id} - with state: ${vicket.state}");
            byte[] messageBytes
            ContentTypeVS contentType = ContentTypeVS.TEXT
            int statusCode = ResponseVS.SC_ERROR_REQUEST
            if(Vicket.State.CANCELLED == vicket.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED
                messageBytes = vicket.cancelMessage.content
            } else if(Vicket.State.EXPENDED == vicket.getState()) {
                contentType = ContentTypeVS.JSON_SIGNED
                messageBytes = vicket.messageSMIME.content
            }
            if(Vicket.State.LAPSED == vicket.getState()) {
                contentType = ContentTypeVS.TEXT
                messageBytes = messageSource.getMessage("vicketLapsedErrorMsg",
                        [vicket.serialNumber].toArray(), locale).getBytes()
            }
            return new ResponseVS(type:TypeVS.ERROR, messageBytes: messageBytes, contentType: contentType,
                    metaInf:MetaInfMsg.getErrorMsg(this.getClass().getSimpleName(), methodName,
                    "VicketState_" + vicket.getState().toString()), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
    }

    public ResponseVS cancelTransactionVS(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessageReq = messageSMIMEReq.getSMIME()
        //messageSMIMEReq?.getSMIME()?.getSigner()?.certificate
        log.debug(smimeMessageReq.getSignedContent())
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = smimeMessageReq.getFrom().toString()
        String subject = messageSource.getMessage('vicketReceiptSubject', null, locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMultiSigned(fromUser, toUser,
                smimeMessageReq, subject)
        messageSMIMEReq.setSMIME(smimeMessageResp)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.VICKET_CANCEL, data:messageSMIMEReq,
                contentType: ContentTypeVS.JSON_SIGNED)
    }

    public ResponseVS processVicketTransaction(VicketTransactionBatch vicketBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        List<Vicket> validatedVicketList = new ArrayList<Vicket>()
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        UserVS toUserVS;
        for(Vicket vicket : vicketBatch.getVicketList()) {
            try {
                toUserVS = UserVS.findWhere(IBAN:vicket.getToUserIBAN())
                if(!toUserVS) throw new ExceptionVS("Error - Vicket with hash '${vicket?.hashCertVS}' has wrong receptor IBAN '" +
                        vicket.getToUserIBAN() + "'", MetaInfMsg.getErrorMsg(methodName, 'toUserVSERROR'))
                vicket.setToUserVS(toUserVS)
                validatedVicketList.add(validateVicket(vicket));
            } catch(Exception ex) {
                String msg = "Error validating Vicket with id '${vicket?.id}' and hash '${vicket?.hashCertVS}'";
                if(ex instanceof ExceptionVS) throw ex
                else throw new ExceptionVS("Error validating Vicket with id '${vicket?.id}' and hash '${vicket?.hashCertVS}'",
                        MetaInfMsg.getErrorMsg(methodName, 'vicketExpended'), ex)
            }
        }
        List responseList = []
        Map currencyMap = [:]
        List<TransactionVS> transactionVSList = []
        for(Vicket vicket: validatedVicketList) {
            Date validTo = null
            if(vicket.isTimeLimited == true) validTo = timePeriod.getDateTo()
            SMIMEMessage receipt = signatureVSService.getSMIMEMultiSigned(systemService.getSystemUser().getName(),
                    vicket.getHashCertVS(), vicket.getSMIME(), vicket.getSubject())
            MessageSMIME messageSMIME = new MessageSMIME(smimeMessage:receipt, type:TypeVS.VICKET_SEND).save()
            TransactionVS transactionVS = new TransactionVS(amount: vicket.amount, messageSMIME:messageSMIME,
                    toUserIBAN:vicket.getToUserIBAN(), state:TransactionVS.State.OK, validTo: validTo,
                    subject:vicket.getSubject(), toUserVS: vicket.getToUserVS(), type:TransactionVS.Type.VICKET_SEND,
                    currencyCode: vicket.getCurrencyCode(), tag:vicket.getTag()).save()
            transactionVSList.add(transactionVS)
            vicket.setState(Vicket.State.EXPENDED).setTransactionVS(transactionVS).save()
            responseList.add([(vicket.getHashCertVS()):Base64.getEncoder().encodeToString(receipt.getBytes())])
        }
        Map resultMap = [statusCode:ResponseVS.SC_OK, message: messageSource.getMessage('vicketSendResultMsg',
                [toUserVS.name, TransactionVSUtils.getBalancesMapMsg(messageSource.getMessage('forLbl', null, locale),
                        TransactionVSUtils.getBalancesMap(transactionVSList))].toArray(),
                locale), receiptList:responseList]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data: resultMap)
    }

    public Vicket validateVicket(Vicket vicket) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessage = vicket.getSMIME()
        Vicket vicketDB =  Vicket.findWhere(serialNumber:vicket.getX509AnonymousCert().serialNumber.longValue(),
                hashCertVS:vicket.getHashCertVS())
        if(!vicketDB) throw new ExceptionVS(messageSource.getMessage('hashCertVSVicketInvalidErrorMsg',
                [vicket.getHashCertVS()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, 'vicketDBMissing'))
        vicket = vicketDB.checkRequestWithDB(vicket)
        if(vicket.state == Vicket.State.EXPENDED) {
            throw new ExceptionVS(messageSource.getMessage('vicketExpendedErrorMsg',
                    [vicket.getHashCertVS()].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, 'vicketExpended'))
        } else if(vicket.state == Vicket.State.OK) {
            UserVS userVS = smimeMessage.getSigner(); //anonymous signer
            CertExtensionCheckerVS extensionChecker
            timeStampService.validateToken(userVS.getTimeStampToken())
            CertUtils.CertValidatorResultVS certValidatorResult = CertUtils.verifyCertificate(signatureVSService.getVicketAnchors(),
                    false, [userVS.getCertificate()])
            X509Certificate certCaResult = certValidatorResult.result.trustAnchor.trustedCert;
            extensionChecker = certValidatorResult.checker
            //if (extensionChecker.isAnonymousSigner()) { }
        } else  throw new ExceptionVS(messageSource.getMessage('vicketStateErrorMsg',
                [vicket.id, vicket.state.toString()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, 'vicketStateError'))
        vicket.setAuthorityCertificateVS(signatureVSService.getServerCertificateVS())
        return vicket
    }

    public ResponseVS processVicketRequest(VicketRequestBatch vicketBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS fromUserVS = vicketBatch.messageSMIME.userVS
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance())
        //Check cash available for user
        ResponseVS<Map<UserVSAccount, BigDecimal>> accountFromMovements =
                walletVSService.getAccountMovementsForTransaction( fromUserVS.IBAN, vicketBatch.getTagVS(),
                vicketBatch.getRequestAmount(), vicketBatch.getCurrencyCode())
        if(ResponseVS.SC_OK != accountFromMovements.getStatusCode()) throw new ValidationExceptionVS(this.getClass(),
                message:accountFromMovements.getMessage(), MetaInfMsg.getErrorMsg(methodName, "lowBalance"))

        vicketBatch = csrService.signVicketBatchRequest(vicketBatch)
        TransactionVS userTransaction = vicketBatch.getTransactionVS(messageSource.getMessage(
                'vicketRequestLbl', null, locale), accountFromMovements.data).save()
        String message = messageSource.getMessage('withdrawalMsg', [vicketBatch.getRequestAmount().toString(),
                vicketBatch.getCurrencyCode()].toArray(), locale) + " " + systemService.getTagMessage(vicketBatch.getTag())

        Map resultMap = [statusCode: ResponseVS.SC_OK, message:message, issuedVickets:vicketBatch.getIssuedVicketListPEM()]
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data:resultMap,
                type:TypeVS.VICKET_REQUEST);
    }

}