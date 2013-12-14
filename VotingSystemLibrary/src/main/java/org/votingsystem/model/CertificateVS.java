package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import javax.persistence.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="CertificateVS")
public class CertificateVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(CertificateVS.class);

    public enum State {OK, ERROR, CANCELLED, USED, UNKNOWN}

    public enum Type {VOTEVS_ROOT, VOTEVS, USER, CERTIFICATE_AUTHORITY, CERTIFICATE_AUTHORITY_TEST, ACTOR_VS}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne private VoteRequestCsrVS voteRequestCsrVS;
    @OneToOne private UserRequestCsrVS userRequestCsrVS;
    @OneToOne(mappedBy="certificateVS") private VoteVS voteVS;
    @Column(name="serialNumber", unique=true, nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) @Lob private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actorVS") private ActorVS actorVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="eventVSElection") private EventVSElection eventVSElection;

    @Column(name="hashCertVoteBase64") private String hashCertVoteBase64;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificateVS") private CertificateVS authorityCertificateVS;

    @Lob @Column(name="certChainPEM") private byte[] certChainPEM;

    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23, insertable=true) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23, insertable=true) private Date lastUpdated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23, insertable=true) private Date validFrom;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23, insertable=true) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="cancelDate", length=23, insertable=true) private Date cancelDate;

    @Transient private String eventId;

    @Transient private String representativeURL;

    @Transient private String serverURL;

    public CertificateVS() {}

    public CertificateVS(X509Certificate x509Certificate) throws IOException {
        String subjectDN = x509Certificate.getSubjectDN().getName();
        log.debug("Certificate - subjectDN: " + subjectDN);
        byte[] eventIdExtensionValue = x509Certificate.getExtensionValue(ContextVS.EVENT_ID_OID);
        if(eventIdExtensionValue != null) {
            DERTaggedObject eventIdDER = (DERTaggedObject) X509ExtensionUtil.fromExtensionValue(eventIdExtensionValue);
            setEventVSId(((DERUTF8String)eventIdDER.getObject()).toString());
        }

        byte[] accesssControlExtensionValue = x509Certificate.getExtensionValue(ContextVS.ACCESS_CONTROL_OID);
        if(accesssControlExtensionValue != null) {
            DERTaggedObject accesssControlDER = (DERTaggedObject)X509ExtensionUtil.fromExtensionValue(
                    accesssControlExtensionValue);
            setServerURL(((DERUTF8String)accesssControlDER.getObject()).toString());
        }

        byte[] hashCertExtensionValue = x509Certificate.getExtensionValue(ContextVS.HASH_CERT_VOTE_OID);
        if (hashCertExtensionValue != null) {
            DERTaggedObject hashCertDER = (DERTaggedObject)X509ExtensionUtil.fromExtensionValue(
                    hashCertExtensionValue);
            String hashCertVoteHex = ((DERUTF8String)hashCertDER.getObject()).toString();
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            setHashCertVoteBase64(new String(hexConverter.unmarshal(hashCertVoteHex)));
        }

        byte[] representativeURLExtensionValue = x509Certificate.getExtensionValue(ContextVS.REPRESENTATIVE_URL_OID);
        if(representativeURLExtensionValue != null) {
            DERTaggedObject representativeURL_DER = (DERTaggedObject)X509ExtensionUtil.fromExtensionValue(
                    representativeURLExtensionValue);
            setRepresentativeURL(((DERUTF8String)representativeURL_DER.getObject()).toString());
        }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getHashCertVoteBase64() {
        return hashCertVoteBase64;
    }

    public void setHashCertVoteBase64(String hashCertVoteBase64) {
        this.hashCertVoteBase64 = hashCertVoteBase64;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public EventVSElection getEventVSElection() {
        return eventVSElection;
    }

    public void setEventVSElection(EventVSElection eventVSElection) {
        this.eventVSElection = eventVSElection;
    }

    public ActorVS getActorVS() {
        return actorVS;
    }

    public void setActorVS(ActorVS actorVS) {
        this.actorVS = actorVS;
    }

    public String getEventVSId() {
        return eventId;
    }

    public void setEventVSId(String eventId) {
        this.eventId = eventId;
    }

    public String getServerURL() {
        return serverURL;
    }


    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public VoteRequestCsrVS getVoteRequestCsrVS() {
        return voteRequestCsrVS;
    }

    public void setVoteRequestCsrVS(VoteRequestCsrVS solicitudCSR) {
        this.voteRequestCsrVS = solicitudCSR;
    }

    public VoteVS getVoteVS() {
        return voteVS;
    }

    public void setVoteVS(VoteVS voteVS) {
        this.voteVS = voteVS;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public CertificateVS getAuthorityCertificateVS() { return authorityCertificateVS; }

    public void setAuthorityCertificateVS(CertificateVS authorityCertificate) {
        this.authorityCertificateVS = authorityCertificate;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() { return validTo; }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public UserRequestCsrVS getUserRequestCsrVS() {
        return userRequestCsrVS;
    }

    public void setUserRequestCsrVS(UserRequestCsrVS userRequestCsrVS) {
        this.userRequestCsrVS = userRequestCsrVS;
    }

    public Date getCancelDate() {
        return cancelDate;
    }

    public void setCancelDate(Date cancelDate) {
        this.cancelDate = cancelDate;
    }

    public String getRepresentativeURL() {
        return representativeURL;
    }

    public void setRepresentativeURL(String representativeURL) {
        this.representativeURL = representativeURL;
    }

    public byte[] getCertChainPEM() {
        return certChainPEM;
    }

    public void setCertChainPEM(byte[] certChain) {
        this.certChainPEM = certChain;
    }

}
