package org.votingsystem.ticket.service

import grails.converters.JSON
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.jce.PKCS10CertificationRequest
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.ticket.TicketVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.StringUtils

import java.security.cert.X509Certificate

class CsrService {

    LinkGenerator grailsLinkGenerator
	def grailsApplication
	def messageSource
    def signatureVSService


    public synchronized ResponseVS signTicket (byte[] csrPEMBytes, String ticketAmount,
           String ticketCurrency, Locale locale) {
        PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        String serverURL = grailsApplication.config.grails.serverURL
        try {
            if(!csr) throw new ExceptionVS(messageSource.getMessage('csrRequestErrorMsg', null, locale))
            CertificationRequestInfo info = csr.getCertificationRequestInfo();
            Enumeration csrAttributes = info.getAttributes().getObjects()
            def certAttributeJSON
            while(csrAttributes.hasMoreElements()) {
                DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
                switch(attribute.getTagNo()) {
                    case ContextVS.TICKET_TAG:
                        String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString()
                        certAttributeJSON = JSON.parse(certAttributeJSONStr)
                        break;
                }
            }

            log.debug("============ info.subject: ${info.subject}")

            // X500Principal subject = new X500Principal("CN=ticketProviderURL:" + ticketProviderURL +"AMOUNT=" + amount + "CURRENCY=" + currency + ", OU=DigitalCurrency");
            if(!certAttributeJSON) throw new ExceptionVS(messageSource.getMessage(
                    'csrMissingDERTaggedObjectErrorMsg', null, locale))
            String ticketProviderURL = StringUtils.checkURL(certAttributeJSON.ticketProviderURL)
            String hashCertVSBase64 = certAttributeJSON.hashCertVS
            String amount = certAttributeJSON.amount
            String currency = certAttributeJSON.currency
            if(!ticketAmount.equals(amount) || !ticketCurrency.equals(currency)) throw new ExceptionVS(
                    messageSource.getMessage('csrTicketValueErrorMsg',
                    ["${ticketAmount} ${ticketCurrency}", "${amount} ${currency}"].toArray(), locale))
            if (!serverURL.equals(ticketProviderURL))  throw new ExceptionVS(messageSource.getMessage(
                    "serverMismatchErrorMsg", [serverURL, ticketProviderURL].toArray(), locale));
            if (!hashCertVSBase64) throw new ExceptionVS(messageSource.getMessage("csrMissingHashCertVSErrorMsg",
                    [serverURL, ticketProviderURL].toArray(), locale));
            //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            //String hashCertVSBase64 = new String(hexConverter.unmarshal(certAttributeJSON.hashCertVS));
            Date certValidFrom = Calendar.getInstance().getTime()
            Date certValidTo = getNextMonday(certValidFrom).getTime()
            X509Certificate issuedCert = signatureVSService.signCSR(csr, null, certValidFrom, certValidTo)
            if (!issuedCert)  throw new ExceptionVS(messageSource.getMessage('csrSigningErrorMsg', null, locale))
            else {
                TicketVS ticketVS = new TicketVS(serialNumber:issuedCert.getSerialNumber().longValue(),
                        content:issuedCert.getEncoded(), state:TicketVS.State.OK, hashCertVS:hashCertVSBase64,
                        validFrom:certValidFrom, validTo: certValidTo).save()
                log.debug("signTicket - expended TicketVS '${ticketVS.id}'")
                byte[] issuedCertPEMBytes = CertUtil.getPEMEncoded(issuedCert);
                Map data = [ticketAmount:ticketAmount, ticketCurrency:ticketCurrency, ticketVS: ticketVS]
                return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.TICKET_REQUEST,
                        data:data, messageBytes:issuedCertPEMBytes)
            }
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(), type:TypeVS.ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR,
                    message:messageSource.getMessage('ticketWithdrawalDataError', null, locale))
        }
    }

    private Calendar getNextMonday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }

    public synchronized ResponseVS signTicketBatchRequest (byte[] ticketBatchRequest, BigDecimal expectedAmount,
            String expectedCurrency, Locale locale){
        ResponseVS responseVS = null;
        String msg = null;
        List<TicketVS> issuedTicketList = new ArrayList<TicketVS>()
        try {
            JSONObject dataRequestJSON = JSON.parse(new String(ticketBatchRequest, "UTF-8"))
            JSONArray ticketsArray = dataRequestJSON.ticketCSR
            List<String> issuedTicketCertList = new ArrayList<String>()
            BigDecimal batchAmount = new BigDecimal(0)
            ticketsArray.each {
                String csr = it.csr
                String ticketCurrency = it.currency
                if(!ticketCurrency.equals(expectedCurrency)) throw new ExceptionVS(messageSource.getMessage(
                        'ticketBatchRequestCurrencyErrorMsg', [expectedCurrency, ticketCurrency].toArray(), locale));
                String ticketAmount = it.ticketValue
                batchAmount = batchAmount.add(new BigDecimal(ticketAmount))
                responseVS = signTicket(csr.getBytes(), ticketAmount, ticketCurrency, locale)
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    issuedTicketList.add(responseVS.data.ticketVS)
                    issuedTicketCertList.add(new String(responseVS.getMessageBytes(), "UTF-8"))
                } else throw new ExceptionVS(responseVS.getMessage())
            }
            if(expectedAmount.compareTo(batchAmount) != 0) throw new ExceptionVS(messageSource.getMessage(
                    'ticketBatchRequestAmountErrorMsg', ["${expectedAmount.toString()} ${expectedCurrency}",
                    "${batchAmount.toString()} ${expectedCurrency}"], locale))
            return new ResponseVS(statusCode:  ResponseVS.SC_OK, data:issuedTicketCertList);
        } catch(ExceptionVS ex) {
            log.error(ex.getMessage(), ex);
            cancelTickets(issuedTicketList, ex.getMessage())
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage(), type:TypeVS.ERROR)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR,
                    message:messageSource.getMessage('ticketWithdrawalDataError', null, locale))
        }
    }

    private void cancelTickets(List<TicketVS> issuedTicketList, String reason) {
        for(TicketVS ticketVS : issuedTicketList) {
            ticketVS.state = TicketVS.State.CANCELLED
            ticketVS.reason = reason
            ticketVS.save()
        }
    }

}
