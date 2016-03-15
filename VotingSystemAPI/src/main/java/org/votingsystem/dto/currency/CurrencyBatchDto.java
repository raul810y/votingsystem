package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.text.MessageFormat;
import java.util.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchDto {

    private TypeVS operation = TypeVS.CURRENCY_SEND;
    private Set<String> currencySet;
    private String leftOverCSR;
    private String currencyChangeCSR;
    private String toUserIBAN;
    private String toUserName;
    private String subject;
    private String currencyCode;
    private String tag;
    private String batchUUID;
    private Boolean timeLimited = Boolean.FALSE;
    private BigDecimal batchAmount;
    private BigDecimal leftOver = BigDecimal.ZERO;
    @JsonIgnore private Currency leftOverCurrency;
    @JsonIgnore private PKCS10CertificationRequest leftOverPKCS10;
    @JsonIgnore private PKCS10CertificationRequest currencyChangePKCS10;
    @JsonIgnore private List<Currency> currencyList;
    @JsonIgnore private byte[] content;


    public CurrencyBatchDto() {}

    public static CurrencyBatchDto FROM_BYTES(byte[] content) throws IOException {
        CurrencyBatchDto currencyBatchDto = JSON.getMapper().readValue(content, CurrencyBatchDto.class);
        currencyBatchDto.setContent(content);
        return currencyBatchDto;
    }

    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUser().getIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.tag = currencyBatch.getTagVS().getName();
        this.timeLimited = currencyBatch.getTimeLimited();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    public static CurrencyBatchDto NEW(String subject, String toUserIBAN, BigDecimal batchAmount, String currencyCode,
            String tag, Boolean timeLimited, List<Currency> currencyList, String currencyServerURL,
            String timeStampServiceURL) throws Exception {
        CurrencyBatchDto batchDto = new CurrencyBatchDto();
        batchDto.subject = subject;
        batchDto.toUserIBAN = toUserIBAN;
        batchDto.batchAmount = batchAmount;
        batchDto.currencyCode = currencyCode;
        batchDto.tag = tag;
        batchDto.timeLimited = timeLimited;
        batchDto.currencyList = currencyList;
        batchDto.batchUUID = UUID.randomUUID().toString();
        BigDecimal accumulated = BigDecimal.ZERO;
        for (Currency currency : currencyList) {
            accumulated = accumulated.add(currency.getAmount());
        }
        if(batchAmount.compareTo(accumulated) > 0) {
            throw new ValidationException(MessageFormat.format("''{0}'' batchAmount exceeds currency sum ''{1}''",
                    batchAmount, accumulated));
        } else if(batchAmount.compareTo(accumulated) != 0){
            batchDto.leftOver = accumulated.subtract(batchAmount);
            batchDto.leftOverCurrency = new Currency(currencyServerURL, batchDto.leftOver, currencyCode, timeLimited,
                    new TagVS(tag));
            batchDto.leftOverCSR = new String(batchDto.leftOverCurrency.getCertificationRequest().getCsrPEM());
        }
        batchDto.currencySet = new HashSet<>();
        for (Currency currency : currencyList) {
            byte[] contentToSign = JSON.getMapper().writeValueAsBytes(
                    CurrencyDto.BATCH_ITEM(batchDto, currency));
            CMSSignedMessage cmsMessage = currency.getCertificationRequest().signDataWithTimeStamp(contentToSign);
            currency.setCMS(cmsMessage);
            batchDto.currencySet.add(currency.getCMS().toPEMStr());
        }
        return batchDto;
    }

    @JsonIgnore
    public CurrencyBatch validateRequest(Date checkDate) throws Exception {
        BigDecimal accumulated = BigDecimal.ZERO;
        BigDecimal wildTagAccumulated = BigDecimal.ZERO;
        currencyList = null;
        for(String currencyItem : currencySet) {
            try {
                Currency currency = new Currency(CMSSignedMessage.FROM_PEM(currencyItem));
                if(currencyList == null) {
                    currencyList = new ArrayList<>();
                }
                checkBatchItem(currency.getBatchItemDto());
                if(checkDate.after(currency.getValidTo())) throw new ValidationException(MessageFormat.format(
                        "currency ''{0}'' is lapsed", currency.getHashCertVS()));
                accumulated = accumulated.add(currency.getAmount());
                if(TagVS.WILDTAG.equals(currency.getTagVS().getName()))
                    wildTagAccumulated = wildTagAccumulated.add(currency.getAmount());
                currencyList.add(currency);
            } catch(Exception ex) {
                throw new ExceptionVS("Error with currency : " + ex.getMessage(), ex);
            }
        }
        if(currencyList == null || currencyList.isEmpty())
            throw new ValidationException("CurrencyBatch without signed transactions");
        CurrencyCertExtensionDto certExtensionDto = null;
        if(leftOverCSR != null) {
            leftOverPKCS10 = PEMUtils.fromPEMToPKCS10CertificationRequest(leftOverCSR.getBytes());
            certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    leftOverPKCS10, ContextVS.CURRENCY_OID);
            if(leftOver.compareTo(certExtensionDto.getAmount()) != 0) throw new ValidationException(
                    "leftOver 'amount' mismatch - request: " + leftOver + " - csr: " + certExtensionDto.getAmount());
            if(!certExtensionDto.getCurrencyCode().equals(currencyCode)) throw new ValidationException(
                    "leftOver 'currencyCode' mismatch - request: " + currencyCode + " - csr: " + certExtensionDto.getCurrencyCode());
            if(!certExtensionDto.getTag().equals(tag)) {
                if(wildTagAccumulated.compareTo(leftOver) < 0) throw new ValidationException(
                    "leftOver 'tag' mismatch - request: " + tag + " - csr: " + certExtensionDto.getTag() +
                    " - wildTagAccumulated: " + wildTagAccumulated);
            }
            BigDecimal leftOverCalculated = accumulated.subtract(batchAmount);
            if(leftOverCalculated.compareTo(leftOver) != 0) throw new ValidationException(
                    "leftOverCalculated: " + leftOverCalculated + " - leftOver: " + leftOver);
        } else if(leftOver.compareTo(BigDecimal.ZERO) != 0) throw new ValidationException(
                "leftOver request: " + leftOver + " without CSR");
        if(currencyChangeCSR != null) {
            currencyChangePKCS10 = PEMUtils.fromPEMToPKCS10CertificationRequest(currencyChangeCSR.getBytes());
            certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    currencyChangePKCS10, ContextVS.CURRENCY_OID);
            if(certExtensionDto.getAmount().compareTo(this.batchAmount) != 0) throw new ValidationException(
                    "currencyChange 'amount' mismatch - request: " + this.batchAmount +
                    " - csr: " + certExtensionDto.getAmount());
            if(!certExtensionDto.getCurrencyCode().equals(currencyCode)) throw new ValidationException(
                    "currencyChange 'currencyCode' mismatch - request: " + currencyCode +
                    " - csr: " + certExtensionDto.getCurrencyCode());
            if(!certExtensionDto.getTag().equals(tag)) throw new ValidationException(
                    "certExtensionDto 'tag' mismatch - request: " + tag + " - csr: " + certExtensionDto.getTag());
            if(timeLimited.booleanValue() !=  certExtensionDto.getTimeLimited().booleanValue())
                throw new ValidationException("certExtensionDto 'timeLimited' mismatch ");
        }
        CurrencyBatch currencyBatch = new CurrencyBatch();
        currencyBatch.setType(operation);
        currencyBatch.setSubject(subject);
        currencyBatch.setBatchAmount(batchAmount);
        currencyBatch.setCurrencyCode(currencyCode);
        currencyBatch.setTagVS(new TagVS(tag));
        currencyBatch.setTimeLimited(timeLimited);
        currencyBatch.setContent(content);
        currencyBatch.setBatchUUID(batchUUID);
        return currencyBatch;
    }

    @JsonIgnore
    public void validateResponse(CurrencyBatchResponseDto responseDto, Set<TrustAnchor> trustAnchor)
            throws Exception {
        CMSSignedMessage receipt = CMSSignedMessage.FROM_PEM(responseDto.getReceipt());
        receipt.isValidSignature();
        CertUtils.verifyCertificate(trustAnchor, false, new ArrayList<>(receipt.getSignersCerts()));
        if(responseDto.getLeftOverCert() != null) {
            leftOverCurrency.initSigner(responseDto.getLeftOverCert().getBytes());
        }
        CurrencyBatchDto signedDto = receipt.getSignedContent(CurrencyBatchDto.class);
        if(signedDto.getBatchAmount().compareTo(batchAmount) != 0) throw new ValidationException(MessageFormat.format(
            "ERROR - batchAmount ''{0}'' - receipt amount ''{1}''",  batchAmount, signedDto.getBatchAmount()));
        if(!signedDto.getCurrencyCode().equals(signedDto.getCurrencyCode())) throw new ValidationException(MessageFormat.format(
             "ERROR - batch currencyCode ''{0}'' - receipt currencyCode ''{1}''",  currencyCode, signedDto.getCurrencyCode()));
        if(timeLimited.booleanValue() != signedDto.getTimeLimited().booleanValue()) throw
                new ValidationException(MessageFormat.format(
                "ERROR - batch timeLimited ''{0}'' - receipt timeLimited ''{1}''",  timeLimited, signedDto.getTimeLimited()));
        if(!tag.equals(signedDto.getTag())) throw new ValidationException(MessageFormat.format(
                "ERROR - batch tag ''{0}'' - receipt tag ''{1}''",  tag, signedDto.getTag()));
        if(!currencySet.equals(signedDto.getCurrencySet())) throw new ValidationException("ERROR - currencySet mismatch");
    }

    @JsonIgnore
    public Map<String, Currency> getCurrencyMap() throws ExceptionVS {
        if(currencyList == null) throw new ExceptionVS("Empty currencyList");
        Map<String, Currency> result = new HashMap<>();
        for(Currency currency : currencyList) {
            result.put(currency.getHashCertVS(), currency);
        }
        return result;
    }

    @JsonIgnore
    public void checkBatchItem(CurrencyDto batchItem) throws ExceptionVS {
        String currencyData = "batchItem with hash '" + batchItem.getHashCertVS() + "' ";
        if(!subject.equals(batchItem.getSubject())) throw new ValidationException(
                currencyData + "expected subject " + subject + " found " + batchItem.getSubject());
        if(toUserIBAN != null) {
            if(!toUserIBAN.equals(batchItem.getToUserIBAN())) throw new ValidationException(
                    currencyData + "expected toUserIBAN " + toUserIBAN + " found " + batchItem.getToUserIBAN());
        }
        if(!currencyCode.equals(batchItem.getCurrencyCode())) throw new ValidationException(
                currencyData + "expected currencyCode " + currencyCode + " found " + batchItem.getCurrencyCode());
        if(!tag.equals(batchItem.getTag())) throw new ValidationException(
                currencyData + "expected tag " + tag + " found " + batchItem.getTag());
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public Set<String> getCurrencySet() {
        return currencySet;
    }

    public void setCurrencySet(Set<String> currencySet) {
        this.currencySet = currencySet;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public List<Currency> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getLeftOverCSR() {
        return leftOverCSR;
    }

    public void setLeftOverCSR(String leftOverCSR) {
        this.leftOverCSR = leftOverCSR;
    }

    public PKCS10CertificationRequest getLeftOverPKCS10() {
        return leftOverPKCS10;
    }

    public void setLeftOverPKCS10(PKCS10CertificationRequest leftOverPKCS10) {
        this.leftOverPKCS10 = leftOverPKCS10;
    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(Currency leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
    }

    public String getCurrencyChangeCSR() {
        return currencyChangeCSR;
    }

    public void setCurrencyChangeCSR(String currencyChangeCSR) {
        this.currencyChangeCSR = currencyChangeCSR;
    }

    public PKCS10CertificationRequest getCurrencyChangePKCS10() {
        return currencyChangePKCS10;
    }

    public void setCurrencyChangePKCS10(PKCS10CertificationRequest currencyChangePKCS10) {
        this.currencyChangePKCS10 = currencyChangePKCS10;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}