package org.votingsystem.model;


import org.votingsystem.crypto.CertUtils;
import org.votingsystem.dto.metadata.MetaInfDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.model.converter.MetaInfConverter;
import org.votingsystem.util.DateUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="CERTIFICATE",
        uniqueConstraints= @UniqueConstraint(columnNames = {"SERIAL_NUMBER", "SUBJECT_DN"}))
@NamedQueries({
        @NamedQuery(name = Certificate.FIND_BY_SERIALNUMBER, query =
                "SELECT c FROM Certificate c WHERE c.serialNumber =:serialNumber"),
        @NamedQuery(name = Certificate.FIND_BY_SERIALNUMBER_AND_AUTHORITY, query =
                "SELECT c FROM Certificate c WHERE c.serialNumber =:serialNumber and c.authorityCertificate=:authorityCertificate"),
        @NamedQuery(name = Certificate.FIND_BY_SIGNER_STATE_AND_TYPE, query =
                "SELECT c FROM Certificate c WHERE c.signer =:signer and c.state =:state and c.type =:type"),
        @NamedQuery(name = Certificate.FIND_BY_SERIALNUMBER_AND_SUBJECT_DN, query =
                "select c from Certificate c where c.serialNumber=:serialNumber and c.subjectDN=:subjectDN"),
})
public class Certificate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(Certificate.class.getName());

    public static final String FIND_BY_SERIALNUMBER_AND_AUTHORITY = "Certificate.findBySerialNumberAndAuthority";
    public static final String FIND_BY_SERIALNUMBER = "Certificate.findBySerialNumber";
    public static final String FIND_BY_SERIALNUMBER_AND_SUBJECT_DN = "Certificate.findBySerialNumberAndSubjectDN";
    public static final String FIND_BY_SIGNER_STATE_AND_TYPE = "Certificate.findBySignerAndStateAndType";

    public enum State {OK, ERROR, CANCELED, CONSUMED, LAPSED, UNKNOWN, SESSION_FINISHED}

    public enum Type {
        VOTE, USER, USER_ID_CARD, CERTIFICATE_AUTHORITY, CERTIFICATE_AUTHORITY_ID_CARD,
        TIMESTAMP_SERVER}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="SERIAL_NUMBER", nullable=false)
    private Long serialNumber;

    @Column(name="CONTENT", nullable=false)
    private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="SIGNER_ID")
    private User signer;

    @Column(name="REVOCATION_HASH_BASE64", unique=true)
    private String revocationHashBase64;

    @Column(name="META_INF", columnDefinition="TEXT")
    @Convert(converter = MetaInfConverter.class)
    private MetaInfDto metaInf;

    @Column(name="SUBJECT_DN")  private String subjectDN;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AUTHORITY_CERTIFICATE_ID") private Certificate authorityCertificate;

    @Column(name="TYPE", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @Column(name="IS_ROOT") private Boolean isRoot;

    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Column(name="VALID_FROM", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validFrom;

    @Column(name="VALID_TO", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validTo;

    @Column(name="STATE_DATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime stateDate;

    @Column(name="CANCEL_DATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime cancelDate;

    @Transient private CertVoteExtensionDto certVoteExtension;


    public Certificate() {}

    public Certificate(X509Certificate x509Cert) throws CertificateEncodingException {
        this.validFrom = DateUtils.getLocalDateFromUTCDate(x509Cert.getNotBefore());
        this.validTo = DateUtils.getLocalDateFromUTCDate(x509Cert.getNotAfter());
        this.content = x509Cert.getEncoded();
        this.serialNumber = x509Cert.getSerialNumber().longValue();
    }

    public static Certificate VOTE(String revocationHashBase64, X509Certificate x509Cert,
            Certificate authorityCertificate) throws CertificateEncodingException {
        Certificate result = new Certificate(x509Cert);
        result.setIsRoot(false);
        result.setState(State.OK).setType(Type.VOTE);
        result.setRevocationHashBase64(revocationHashBase64);
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.authorityCertificate = authorityCertificate;
        return result;
    }


    public static Certificate AUTHORITY(X509Certificate x509Cert, Type type, MetaInfDto metaInf) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        result.isRoot = CertUtils.isSelfSigned(x509Cert);
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.type = type;
        result.state = Certificate.State.OK;
        result.metaInf = metaInf;
        return result;
    }

    public static Certificate SIGNER(User signer, X509Certificate x509Cert)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        if(signer.getCertificateCA() != null &&
                Type.CERTIFICATE_AUTHORITY_ID_CARD == signer.getCertificateCA().getType()) result.type = Type.USER_ID_CARD;
        else result.type = Type.USER;
        result.state = Certificate.State.OK;
        result.signer = signer;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.authorityCertificate = signer.getCertificateCA();
        return result;
    }

    public static Certificate ISSUED_USER_CERT(User user, X509Certificate x509Cert, Certificate authorityCertificate)
            throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        Certificate result = new Certificate(x509Cert);
        if(Type.CERTIFICATE_AUTHORITY_ID_CARD == authorityCertificate.getType()) result.type = Type.USER_ID_CARD;
        else result.type = Type.USER;
        result.state = Certificate.State.OK;
        result.signer = user;
        result.subjectDN = x509Cert.getSubjectDN().toString();
        result.authorityCertificate = authorityCertificate;
        return result;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public User getSigner() {
        return signer;
    }

    public void setSigner(User user) {
        this.signer = user;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getRevocationHashBase64() {
        return revocationHashBase64;
    }

    public void setRevocationHashBase64(String hashCertVSBase64) {
        this.revocationHashBase64 = hashCertVSBase64;
    }

    public State getState() {
        return state;
    }

    public Certificate setState(State state) {
        this.state = state;
        return this;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Certificate getAuthorityCertificate() { return authorityCertificate; }

    public Certificate setAuthorityCertificate(Certificate authorityCertificate) {
        this.authorityCertificate = authorityCertificate;
        return this;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() { return validTo; }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getCancelDate() {
        return cancelDate;
    }

    public void setCancelDate(LocalDateTime cancelDate) {
        this.cancelDate = cancelDate;
    }

    public Boolean getIsRoot() {
        return isRoot;
    }

    public void setIsRoot(Boolean isRoot) {
        this.isRoot = isRoot;
    }

    public MetaInfDto getMetaInf() {
        return metaInf;
    }

    public void setMetaInf(MetaInfDto metaInf) {
        this.metaInf = metaInf;
    }

    public X509Certificate getX509Certificate() throws Exception {
        return CertUtils.loadCertificate(content);
    }

    public LocalDateTime getStateDate() {
        return stateDate;
    }

    public void setStateDate(LocalDateTime stateDate) {
        this.stateDate = stateDate;
    }

    public CertVoteExtensionDto getCertVoteExtension() {
        return certVoteExtension;
    }

    public Certificate setCertVoteExtension(CertVoteExtensionDto certVoteExtension) {
        this.certVoteExtension = certVoteExtension;
        return this;
    }

}