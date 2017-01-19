package org.votingsystem.model.currency;

import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.crypto.HashUtils;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.throwable.CertificateRequestException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.DateUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CURRENCY")
public class Currency extends EntityBase implements Serializable  {

    public static final long serialVersionUID = 1L;

    //Lapsed -> for not expended time limited currency
    public enum State { OK, EXPENDED, LAPSED, UNKNOWN, ERROR_CERT_AUTHORITY, ERROR;}

    public enum Type { LEFT_OVER, CHANGE, REQUEST }

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;
    
    @Column(name="SUBJECT") 
    private String subject;
    
    @Column(name="AMOUNT") 
    private BigDecimal amount = null;
    
    @Column(name="CURRENCY", nullable=false) @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode;

    @Column(name="IS_TIME_LIMITED")
    private Boolean timeLimited = Boolean.FALSE;

    @Column(name="REVOCATION_HASH_BASE64")
    private String revocationHashBase64;
    
    @Column(name="ORIGIN_REVOCATION_HASH") 
    private String originRevocationHash;
    
    @Column(name="CURRENCY_SERVER_ENTITY_ID") 
    private String currencyServerEntityId;

    @Column(name="REASON")
    private String reason;

    @Column(name="META_INF") 
    private String metaInf;
    
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="CURRENCY_BATCH") 
    private CurrencyBatch currencyBatch;

    @Column(name="SERIAL_NUMBER", unique=true, nullable=false) 
    private Long serialNumber;
    
    @Column(name="CONTENT", nullable=false) 
    private byte[] content;
    
    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING) 
    private State state;
    
    @Column(name="TYPE", nullable=false) @Enumerated(EnumType.STRING) 
    private Type type;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="TO_USER") 
    private User toUser;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AUTHORITY_CERTIFICATE") 
    private Certificate authorityCertificate;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TAG", nullable=false)
    private Tag tag;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TRANSACTION")
    private Transaction transaction;

    @OneToOne @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;

    @Column(name="VALID_FROM", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validFrom;

    @Column(name="VALID_TO", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validTo;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @Transient private CertificationRequest certificationRequest;
    @Transient private X509Certificate x509AnonymousCert;
    @Transient private String toUserIBAN;
    @Transient private String toUserName;
    @Transient private CurrencyDto batchItemDto;
    @Transient private CurrencyCertExtensionDto certExtensionDto;

    public Currency() {}

    public Currency(SignedDocument signedDocument) throws Exception {
        this.signedDocument = signedDocument;
        initCertData(signedDocument.getCurrencyCert());
        batchItemDto = signedDocument.getSignedContent(CurrencyDto.class);
        if(!this.currencyCode.equals(batchItemDto.getCurrencyCode())) {
            throw new IllegalArgumentException(getErrorPrefix() +
                    "expected currencyCode '" + currencyCode + "' - found: '" + batchItemDto.getCurrencyCode());
        }
        if(!Tag.WILDTAG.equals(certExtensionDto.getTag()) && !certExtensionDto.getTag().equals(batchItemDto.getTag()))
            throw new IllegalArgumentException("expected tag '" + certExtensionDto.getTag() + "' - found: '" +
                    batchItemDto.getTag());
        Date signatureTimeUTC = DateUtils.getUTCDate(signedDocument.getFirstSignature().getSignatureDate());
        if(signatureTimeUTC.after(x509AnonymousCert.getNotAfter())) throw new ValidationException(getErrorPrefix() +
                "valid to '" + DateUtils.getUTCDateStr(x509AnonymousCert.getNotAfter()) + "' has signature date '" +
                DateUtils.getUTCDateStr(signatureTimeUTC)+ "'");
        this.subject = batchItemDto.getSubject();
        this.toUserIBAN = batchItemDto.getToUserIBAN();
        this.toUserName = batchItemDto.getToUserName();
    }

    public Currency(String currencyEntity, BigDecimal amount, CurrencyCode currencyCode, Boolean timeLimited,
                String revocationHashBase64, Tag tag) throws NoSuchAlgorithmException, CertificateRequestException  {
        this.amount = amount;
        this.currencyServerEntityId = currencyEntity;
        this.currencyCode = currencyCode;
        this.tag = tag;
        this.timeLimited = timeLimited;
        this.revocationHashBase64 = revocationHashBase64;
        certificationRequest = CertificationRequest.getCurrencyRequest(currencyEntity, revocationHashBase64,
                amount, currencyCode, timeLimited, tag.getName());
    }

    public Currency(String currencyEntity, BigDecimal amount, CurrencyCode currencyCode, Boolean timeLimited, Tag tag)
            throws NoSuchAlgorithmException, CertificateRequestException {
        this.amount = amount;
        this.currencyServerEntityId = currencyEntity;
        this.currencyCode = currencyCode;
        this.tag = tag;
        this.timeLimited = timeLimited;
        this.originRevocationHash = UUID.randomUUID().toString();
        this.revocationHashBase64 = HashUtils.getHashBase64(this.originRevocationHash.getBytes(),
                Constants.DATA_DIGEST_ALGORITHM);
        certificationRequest = CertificationRequest.getCurrencyRequest(currencyEntity, revocationHashBase64,
                amount, currencyCode, timeLimited, tag.getName());
    }

    public static Currency FROM_CERT(X509Certificate currencyCert, Tag tag,
                                     Certificate authorityCertificate) throws Exception {
        Currency currency = new Currency().setState(State.OK).setTag(tag).setAuthorityCertificate(authorityCertificate);
        currency.initCertData(currencyCert);
        return currency;
    }

    public CurrencyDto getBatchItemDto() {
        return batchItemDto;
    }

    public void setBatchItemDto(CurrencyDto batchItemDto) {
        this.batchItemDto = batchItemDto;
    }

    public Currency initCertData(X509Certificate x509AnonymousCert) throws Exception {
        this.x509AnonymousCert = x509AnonymousCert;
        content = x509AnonymousCert.getEncoded();
        serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, Constants.CURRENCY_OID);
        if(certExtensionDto == null)
            throw new ValidationException("error missing cert extension data");
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        revocationHashBase64 = certExtensionDto.getHashCertVS();
        timeLimited = certExtensionDto.getTimeLimited();
        tag = new Tag(certExtensionDto.getTag());
        currencyServerEntityId = certExtensionDto.getCurrencyServerURL();
        validFrom = DateUtils.getLocalDateFromUTCDate(x509AnonymousCert.getNotBefore());
        validTo = DateUtils.getLocalDateFromUTCDate(x509AnonymousCert.getNotAfter());
        String subjectDN = x509AnonymousCert.getSubjectDN().toString();
        CurrencyDto certSubjectDto = CurrencyDto.getCertSubjectDto(subjectDN, revocationHashBase64);
        if(!certSubjectDto.getCurrencyServerURL().equals(certExtensionDto.getCurrencyServerURL()))
            throw new ValidationException("currencyServerEntityId: " + currencyServerEntityId + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationException("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getTag().equals(certExtensionDto.getTag()))
            throw new ValidationException("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        return this;
    }

    public Currency checkRequestWithDB(Currency currencyRequest) throws ValidationException {
        if(!currencyRequest.getCurrencyServerEntityId().equals(currencyServerEntityId))
            throw new ValidationException("Expected Currency server: " + currencyServerEntityId +
                    " - found: " + currencyRequest.getCurrencyServerEntityId());
        if(!currencyRequest.getRevocationHash().equals(revocationHashBase64))
            throw new ValidationException("Expected revocation hash: " + revocationHashBase64 +
                    " - found: " + currencyRequest.getRevocationHash());
        if(!currencyRequest.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("Expected currency code: " + currencyCode + " - found: " +
                    currencyRequest.getCurrencyCode());
        if (!currencyRequest.getCertExtensionDto().getTag().equals(tag.getName()))
            throw new ValidationException("Expected tag: " + tag.getName() +
                    " - found: " + currencyRequest.getCertExtensionDto().getTag());
        if(currencyRequest.getAmount().compareTo(amount) != 0)  throw new ValidationException(
                "Expected amount: " + amount + " - found: " + currencyRequest.getAmount());
        this.signedDocument = currencyRequest.getSignedDocument();
        this.x509AnonymousCert = currencyRequest.getX509AnonymousCert();
        this.toUser = currencyRequest.getToUser();
        this.toUserIBAN = currencyRequest.getToUserIBAN();
        this.subject = currencyRequest.getSubject();
        return this;
    }

    public CurrencyBatch getCurrencyBatch() {
        return currencyBatch;
    }

    public Currency setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
        return this;
    }

    public Type getType() {
        return type;
    }

    public Currency setType(Type type) {
        this.type = type;
        return this;
    }

    public void setX509AnonymousCert(X509Certificate x509AnonymousCert) {
        this.x509AnonymousCert = x509AnonymousCert;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    private String getErrorPrefix() {
        return "ERROR - Currency with revocation hash: " + revocationHashBase64 + " - ";
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Tag getTag() {
        return tag;
    }

    public Currency setTag(Tag tag) {
        this.tag = tag;
        return this;
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        certificationRequest.setSignedCsr(csrBytes);
        initCertData(certificationRequest.getCertificate());
    }

    public static Currency fromCertificationRequest(CertificationRequest certificationRequest) throws Exception {
        Currency currency = new Currency();
        currency.setCertificationRequest(certificationRequest);
        currency.initSigner(certificationRequest.getCsrCertificate());
        return currency;
    }

    public CurrencyCertExtensionDto getCertExtensionDto() {
        return certExtensionDto;
    }

    public void setCertExtensionDto(CurrencyCertExtensionDto certExtensionDto) {
        this.certExtensionDto = certExtensionDto;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public String getToUserName() {
        return toUserName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public X509Certificate getX509AnonymousCert() {
        return x509AnonymousCert;
    }

    public String getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(String metaInf) {
        this.metaInf = metaInf;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRevocationHash() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String revocationHash) {
        this.revocationHashBase64 = revocationHashBase64;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyServerEntityId() {
        return currencyServerEntityId;
    }

    public void setCurrencyServerEntityId(String currencyServerEntityId) {
        this.currencyServerEntityId = currencyServerEntityId;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
    }

    @Override
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Certificate getAuthorityCertificate() {
        return authorityCertificate;
    }

    public Currency setAuthorityCertificate(Certificate authorityCertificate) {
        this.authorityCertificate = authorityCertificate;
        return this;
    }

    public State getState() {
        return state;
    }

    public Currency setState(State state) {
        this.state = state;
        return this;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Currency setTransaction(Transaction transactionParent) {
        this.transaction = transactionParent;
        return this;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User user) {
        this.toUser = user;
    }

    public CertificationRequest getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequest certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

}