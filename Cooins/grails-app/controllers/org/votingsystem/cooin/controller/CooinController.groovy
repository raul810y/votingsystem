package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.apache.log4j.Logger
import org.apache.log4j.RollingFileAppender
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.cooin.model.Cooin
import org.votingsystem.cooin.model.CooinRequestBatch
import org.votingsystem.model.ContextVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

class CooinController {

    private static Logger cooinsIssuedlog = Logger.getLogger("cooinsIssuedLog");

    def cooinService
    def userVSService
    def transactionVSService


    def request() {
        render(view:'request')
    }

    def issuedLog() {
        if(request.contentType?.contains("json")) {
            RollingFileAppender appender = cooinsIssuedlog.getAppender("CooinsIssued")
            File reportsFile = new File(appender.file)
            //testfile.eachLine{ line ->}
            def messageJSON = JSON.parse("{" + reportsFile.text + "}")
            render messageJSON as JSON
            return false
        } else {
            render(view:'issued')
        }
    }

    def cancel() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS:cooinService.cancelCooin(messageSMIME)]
    }


    def cancelBatch () {
        JSONObject jsonRequest = JSON.parse(new String(params.requestBytes))
        JSONArray cooinCancellationArray = jsonRequest.getJSONArray("cooinCancellationList")
        cooinCancellationArray.each {
            log.debug("cooinCancellationArray -it: ${it}")
        }
    }

    /**
     * Service that validates cash requests
     *
     * @httpMethod [POST]
     * @serviceURL [/cooin/request]
     * @requestContentType [application/pkcs7-signature] The request with the amount required signed by the user
     * @param [csr] Required. The anonymous certificate request for the cash.
     * @return The anonymous certificate request signed (with this you can make secured anonymous transactions).
     */
    def processRequestFileMap() {
        MessageSMIME messageSMIMEReq = params[ContextVS.COOIN_REQUEST_DATA_FILE_NAME]
        request.messageSMIMEReq = messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }

        CooinRequestBatch cooinBatch = new CooinRequestBatch(params[ContextVS.CSR_FILE_NAME], messageSMIMEReq,
                grailsApplication.config.grails.serverURL)
        if(!cooinBatch.tagVS) {
            TagVS.withTransaction {
                TagVS tagVS = TagVS.findWhere(name:cooinBatch.getTag())
                cooinBatch.setTagVS(tagVS)
            }
        }
        return [responseVS:cooinService.processCooinRequest(cooinBatch)]
    }

    def wallet() {}

    def status() {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(params.hashCertVSHex))
        Cooin cooin
        Cooin.withTransaction {
            cooin = Cooin.findWhere(hashCertVS:hashCertVSBase64)
        }
        int statusCode = ResponseVS.SC_MESSAGE_FROM_VS
        String msg = null
        if(!cooin) msg = message(code:'cooinNotFoundErrorMsg')
        else {
            switch(cooin.state) {
                case Cooin.State.EXPENDED:
                    msg = message(code:'cooinExpendedShortErrorMsg')
                    break;
                case Cooin.State.CANCELLED:
                    msg = message(code:'cooinCancelledErrorMsg')
                    break;
                case Cooin.State.OK:
                    if(cooin.validTo.after(Calendar.getInstance().getTime())) {
                        statusCode = ResponseVS.SC_OK
                        msg = message(code:'cooinOKMsg')
                        break;
                    } else {
                        cooin.state = Cooin.State.LAPSED
                        Cooin.withTransaction { cooin.save()}
                    }
                case Cooin.State.LAPSED:
                    msg = message(code:'cooinLapsedShortErrorMsg')
                    break;
            }
        }
        response.status = statusCode
        render msg
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}