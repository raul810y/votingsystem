package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.BatchRequest
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.vicket.Vicket
import org.votingsystem.model.vicket.VicketBatchRequest
import org.votingsystem.model.vicket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class TransactionController {

    def userVSService
    def transactionVSService
    def signatureVSService
    def vicketService

    def index() {
        Map sortParamsMap = org.votingsystem.groovy.util.StringUtils.getSortParamsMap(params)
        Map.Entry sortParam
        if(!sortParamsMap.isEmpty()) sortParam = sortParamsMap?.entrySet()?.iterator()?.next()
        List<TransactionVS> transactionList = null
        int totalTransactions = 0;
        TransactionVS.withTransaction {
            if(params.searchParam) {
                CurrencyVS currency = null
                TransactionVS.Type transactionType = null
                BigDecimal amount = null
                try {currency = CurrencyVS.valueOf(params.searchParam.toUpperCase())} catch(Exception ex) {}
                try {transactionType = TransactionVS.Type.valueOf(params.searchParam.toUpperCase())} catch(Exception ex) {}
                try {amount = new BigDecimal(params.searchParam)} catch(Exception ex) {}
                transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset,
                        sort:sortParam?.key, order:sortParam?.value) {
                    or {
                        if(currency) eq("currency", currency)
                        if(transactionType) eq("type", transactionType)
                        if(amount) eq("amount", amount)
                        ilike('subject', "%${params.searchParam}%")
                    }
                }
                totalTransactions = transactionList.totalCount
            } else {
                transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset,
                        sort:sortParam?.key, order:sortParam?.value){};
                totalTransactions = transactionList.totalCount
            }
        }
        def resultList = []
        transactionList.each {transactionItem ->
            resultList.add(transactionVSService.getTransactionMap(transactionItem))
        }
        def resultMap = ["${message(code: 'transactionRecordsLbl')}":resultList, queryRecordCount: totalTransactions,
                        numTotalTransactions:totalTransactions ]
        render resultMap as JSON
    }

    def listener() {

    }


    /**
     * Servicio que recibe una transacción compuesta por un lote de Vickets
     *
     * @httpMethod [POST]
     * @serviceURL [/transaction/vicketBatch]
     * @requestContentType Documento JSON con la extructura https://github.com/jgzornoza/SistemaVotacion/wiki/Lote-de-Vickets
     * @responseContentType [application/pkcs7-mime]. Documento JSON cifrado en el que figuran los recibos de los vicket recibidos.
     * @return
     */
    def vicketBatch() {
        if(!params.requestBytes) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        VicketBatchRequest batchRequest;
        VicketBatchRequest.withTransaction {
            batchRequest = new VicketBatchRequest(state:BatchRequest.State.OK, content:params.requestBytes,
                    type: TypeVS.VICKET_REQUEST).save()
        }
        def requestJSON = JSON.parse(new String(params.requestBytes, ContextVS.UTF_8))
        byte[] decodedPK = Base64.decode(requestJSON.publicKey);
        PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
        JSONArray vicketsArray = requestJSON.vickets
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK)
        byte[] bytesResponse
        List<ResponseVS> responseList = new ArrayList<ResponseVS>()
        for(int i = 0; i < vicketsArray.size(); i++) {
            SMIMEMessageWrapper smimeMessageReq = new SMIMEMessageWrapper(new ByteArrayInputStream(
                    Base64.decode(vicketsArray.getString(i).getBytes())))
            ResponseVS signatureResponse = signatureVSService.processSMIMERequest(smimeMessageReq, ContentTypeVS.VICKET,
                    request.getLocale())
            if(ResponseVS.SC_OK == signatureResponse.getStatusCode()) {
                responseList.add(signatureResponse);
            } else {
                responseVS = signatureResponse
                break;
            }
        }
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            String msg = message(code: "vicketBatchErrorMsg") + "--- ${responseVS.getMessage()}"
            cancelVicketBatchRequest(responseList, batchRequest, TypeVS.VICKET_SIGNATURE_ERROR, msg)
            return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    type: TypeVS.VICKET_SIGNATURE_ERROR,
                    contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
        } else {
            List<ResponseVS> depositResponseList = new ArrayList<ResponseVS>()
            for(ResponseVS response : responseList) {
                ResponseVS depositResponse = vicketService.processVicketDeposit(
                        response.data, batchRequest, request.locale)
                if(ResponseVS.SC_OK == depositResponse.getStatusCode()) {
                    depositResponseList.add(depositResponse);
                } else {
                    responseVS = depositResponse
                    break;
                }
            }
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                    cancelVicketBatchRequest(responseList, batchRequest, TypeVS.VICKET_REQUEST_WITH_ITEMS_REPEATED,
                            responseVS.data.message)
                    cancelVicketBatchDeposit(depositResponseList, batchRequest,TypeVS.VICKET_REQUEST_WITH_ITEMS_REPEATED,
                            responseVS.data.message)
                    return [receiverPublicKey:receiverPublic, responseVS:responseVS];
                } else {
                    String msg = message(code: "vicketBatchErrorMsg") + " ${responseVS.getMessage()}"
                    cancelVicketBatchRequest(responseList, batchRequest, TypeVS.VICKET_BATCH_ERROR, msg)
                    cancelVicketBatchDeposit(depositResponseList, batchRequest,TypeVS.VICKET_BATCH_ERROR, msg)
                    return [receiverPublicKey:receiverPublic,  responseVS:new ResponseVS(
                            statusCode:responseVS.getStatusCode(), type:TypeVS.VICKET_BATCH_ERROR,
                            contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
                }
            } else {
                List<String> vicketReceiptList = new ArrayList<String>()
                for(ResponseVS response: depositResponseList) {
                    //Map dataMap = [vicketReceipt:messageSMIMEResp, vicket:vicket]
                    vicketReceiptList.add(new String(Base64.encode(((MessageSMIME)response.getData().vicketReceipt).content)))
                }
                Map responseMap = [vickets:vicketReceiptList]
                byte[] responseBytes = "${responseMap as JSON}".getBytes()
                return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                        contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: responseBytes)]
            }
        }
    }

    private void cancelVicketBatchDeposit(List<ResponseVS> responseList, VicketBatchRequest batchRequest, TypeVS typeVS,
                                          String reason) {
        log.error("cancelVicketBatchDeposit - batchRequest: '${batchRequest.id}' - reason: ${reason} - type: ${typeVS}")
        for(ResponseVS responseVS: responseList) {
            if(responseVS.data instanceof Map) {
                ((MessageSMIME)responseVS.data.vicketReceipt).type = typeVS
                ((MessageSMIME)responseVS.data.vicketReceipt).reason = reason
                ((MessageSMIME)responseVS.data.vicketReceipt).save()
                ((Vicket)responseVS.data.vicket).setState(Vicket.State.OK)
                ((Vicket)responseVS.data.vicket).save()
            } else log.error("cancelVicketBatch unknown data type ${responseVS.data.getClass().getName()}")
        }
        batchRequest.setType(typeVS)
        batchRequest.setState(BatchRequest.State.ERROR)
        batchRequest.setReason(reason)
        batchRequest.save()
    }

    private void cancelVicketBatchRequest(List<ResponseVS> responseList, VicketBatchRequest batchRequest, TypeVS typeVS,
               String reason) {
        log.error("cancelVicketBatch - batchRequest: '${batchRequest.id}' - reason: ${reason} - type: ${typeVS}")
        for(ResponseVS responseVS: responseList) {
            if(responseVS.data instanceof MessageSMIME) {
                ((MessageSMIME)responseVS.data).batchRequest = batchRequest
                ((MessageSMIME)responseVS.data).type = typeVS
                ((MessageSMIME)responseVS.data).reason = reason
                ((MessageSMIME)responseVS.data).save()
            } else log.error("cancelVicketBatch unknown data type ${responseVS.data.getClass().getName()}")
        }
        batchRequest.setType(typeVS)
        batchRequest.setState(BatchRequest.State.ERROR)
        batchRequest.setReason(reason)
        batchRequest.save()
    }

    /**
     * Servicio que recibe los asignaciones de los usuarios en documentos SMIME
     *
     * @httpMethod [POST]
     * @serviceURL [/transaction/deposit]
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio.
     *                     documento SMIME firmado con un vicket emitido por el sistema.
     * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
     * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
     */
    def deposit() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
        ResponseVS responseVS = null
        if(ContentTypeVS.VICKET == contentTypeVS) {
            responseVS = vicketService.processVicketDeposit(messageSMIMEReq, null, request.locale)
        } else responseVS = transactionVSService.processDeposit(messageSMIMEReq, request.locale)
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }

}
