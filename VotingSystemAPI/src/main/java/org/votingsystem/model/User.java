package org.votingsystem.model;

import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.util.EntityVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.CMSUtils;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="UserVS")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="userType", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("User")
@NamedQueries({
        @NamedQuery(name = "findUserByType", query = "SELECT u FROM User u WHERE u.type =:type"),
        @NamedQuery(name = "findUserByNIF", query = "SELECT u FROM User u WHERE u.nif =:nif"),
        @NamedQuery(name = "findUserByIBAN", query = "SELECT u FROM User u WHERE u.IBAN =:IBAN"),
        @NamedQuery(name = "findUserActiveOrCancelledAfterAndInList", query = "SELECT u FROM User u " +
                "WHERE u.type in :typeList and(u.state = 'ACTIVE' or (u.state in :notActiveList and(u.dateCancelled >=:dateCancelled)))"),
        @NamedQuery(name = "findUserByRepresentativeAndIBAN", query = "SELECT u FROM User u " +
                "WHERE u.representative =:representative and u.IBAN =:IBAN"),
        @NamedQuery(name = "countUserActiveByDateAndInList", query = "SELECT COUNT(u) FROM User u " +
                "WHERE (u.dateCancelled is null OR u.dateCancelled >=:date) and u.type in :inList")
})
public class User extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(User.class.getName());

    public enum Type {USER, GROUP, SYSTEM, REPRESENTATIVE, BANK}

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
	@Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type = Type.USER;
    @Column(name="nif", unique=true) private String nif;
    @Column(name="IBAN") private String IBAN;
    @Column(name="name") private String name;
    @Column(name="url") private String url;
    @Column(name="metaInf", columnDefinition="TEXT") private String metaInf = "{\"numRepresentations\":1}";
    @Column(name="firstName" ) private String firstName;
    @Column(name="lastName" ) private String lastName;
    @Column(name="description", columnDefinition="TEXT" ) private String description;
    @Column(name="country" ) private String country;
    @Column(name="phone" ) private String phone;
    @Column(name="email" ) private String email;
    @Column(name="cn") private String cn;
    @Column(name="reason") private String reason;
    @Column(name="state") @Enumerated(EnumType.STRING) private State state = State.ACTIVE;
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="representative") private User representative;
    @OneToOne private Address address;
    //Owning Entity side of the relationship
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinTable(name = "user_tagvs", joinColumns = {
            @JoinColumn(name = "userId", referencedColumnName = "id", nullable = false) },
            inverseJoinColumns = { @JoinColumn(name = "tagvs", nullable = false, referencedColumnName = "id") })
    private Set<TagVS> tagVSSet;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCancelled", length=23) private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateActivated", length=23) private Date dateActivated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
    
    @Transient private transient X509Certificate x509Certificate;
    @Transient private transient Certificate certificate;
    @Transient private transient Certificate certificateCA;
    @Transient private transient Map metaInfMap;
    @Transient private transient TimeStampToken timeStampToken;
    @Transient private transient SignerInformation signerInformation;
    @Transient private transient Device device;
    @Transient private KeyStore keyStore;
    @Transient private boolean isAnonymousUser;

    public User() {}

    public User(String nif, String name, Type type) {
        this.nif = nif;
        this.name = name;
        this.type = type;
    }

    public String getIBAN() {
        return IBAN;
    }

    public User setIBAN(String IBAN) {
        this.IBAN = IBAN;
        return this;
    }

    public User(String nif) { this.nif = nif; }
    public User(String nif, Type type, String name) {
        this.nif = nif;
        this.type = type;
        this.name = name;
    }

    public User(String nif, Type type, String name, String firstName, String lastName, String email, String phone) {
        this.nif = nif;
        this.type = type;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public static User REPRESENTATIVE(String representativeURL) {
        User result = new User();
        result.setType(User.Type.REPRESENTATIVE).setUrl(representativeURL);
        return result;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public SignerInformation getSignerInformation() {
        return signerInformation;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
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

    public String getName() {
        if(name == null) name = firstName + " " + lastName;
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setFirstName(String firstName) {this.firstName = firstName;}

    public String getFirstName() {
        return firstName;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return cn;
    }

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Certificate getCertificateCA() {
		return certificateCA;
	}

	public void setCertificateCA(Certificate certificate) {
		this.certificateCA = certificate;
	}

	public User getRepresentative() {
		return representative;
	}

	public User setRepresentative(User representative) {
		this.representative = representative;
        return this;
	}

	public Type getType() {
		return type;
	}

	public User setType(Type type) {
		this.type = type;
        return this;
	}

    public String getReason() {
        return reason;
    }

    public User setReason(String reason) {
        this.reason = reason;
        return this;
    }

	public TimeStampToken getTimeStampToken() {
		return timeStampToken;
	}

	public void setTimeStampToken(TimeStampToken timeStampToken) {
		this.timeStampToken = timeStampToken;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}

    public void updateAdmins(Set<String> admins) throws IOException {
        getMetaInfMap().put("adminsDNI", admins);
        this.metaInf = JSON.getMapper().writeValueAsString(metaInfMap);
    }

    public void setSignerInformation(SignerInformation signer) {
        this.signerInformation = signer;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public void setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
    }

    public boolean isAnonymousUser() {
        return isAnonymousUser;
    }

    public void setAnonymousUser(boolean anonymousUser) {
        isAnonymousUser = anonymousUser;
    }

    public String getDescription() {
        return description;
    }

    public User setDescription(String description) {
        this.description = description;
        return this;
    }

    public Date getDateActivated() {
        return dateActivated;
    }

    public void setDateActivated(Date dateActivated) {
        this.dateActivated = dateActivated;
    }

    public State getState() {
        return state;
    }

    public User setState(State state) {
        this.state = state;
        return this;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Set<TagVS> getTagVSSet() {
        return tagVSSet;
    }

    public void setTagVSSet(Set<TagVS> tagVSSet) {
        this.tagVSSet = tagVSSet;
    }

    public String getSignatureBase64() {
        if (signerInformation.getSignature() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getSignature());
    }

    public String getSignatureHex() {
        if (signerInformation.getSignature() == null) return null;
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(getSignatureBase64().getBytes());
    }

    public String getEncryptiontId() {
        if(signerInformation == null) return null;
        else return CMSUtils.getEncryptiontId(signerInformation.getEncryptionAlgOID()); 
    }

    public Date getSignatureDate() {
        if(timeStampToken == null) return null;
        return timeStampToken.getTimeStampInfo().getGenTime();
    }

    public String getDigestId() {
        if(signerInformation == null) return null;
        else return CMSUtils.getDigestId(signerInformation.getDigestAlgOID()); }

    public String getContentDigestHex() {
        if (signerInformation.getContentDigest() == null) return null;
        HexBinaryAdapter hexConverter = new HexBinaryAdapter();
        return hexConverter.marshal(signerInformation.getContentDigest());
    }

    public void setSigner(SignerInformation signer) {
        this.signerInformation = signer;
    }

    public String getSignedContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return DatatypeConverter.printBase64Binary(signerInformation.getContentDigest());
    }

    public static String getServerInfoURL(String serverURL, Long userId) {
        return serverURL + "/rest/user/id/" + userId;
    }

    public Map getMetaInfMap() throws IOException {
        if(metaInfMap == null) {
            if(metaInf == null) {
                metaInfMap = new HashMap<>();
                metaInf = JSON.getMapper().writeValueAsString(metaInfMap);
            } else metaInfMap = JSON.getMapper().readValue(metaInf, HashMap.class);
        }
        return metaInfMap;
    }

    public User updateCertInfo (X509Certificate certificate) {
        User user = getUser(certificate.getSubjectDN().getName());
        setFirstName(user.getFirstName());
        setName(user.getFirstName());
        setLastName(user.getLastName());
        setNif(user.getNif());
        setCountry(user.getCountry());
        setCn(user.getCn());
        return this;
    }

    public static User FROM_X509_CERT(X509Certificate certificate) {
        User user = getUser(certificate.getSubjectDN().getName());
        user.setX509Certificate(certificate);
        return user;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }


    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getFullName() {
        if(firstName == null) return name;
        else return firstName + " " + lastName;
    }

    public static User getUser(String subjectDN) {
        User user = new User();
        if (subjectDN.contains("C=")) user.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER=")) user.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME=")) user.setLastName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME=")) {
            String givenname = subjectDN.split("GIVENNAME=")[1].split(",")[0];
            user.setName(givenname);
            user.setFirstName(givenname);
        }
        if (subjectDN.contains("CN=")) user.setCn(subjectDN.split("CN=")[1]);
        return user;
    }

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
        if(nif != null) {
            this.nif = nif.toUpperCase();
        }
        if(name == null) {
            this.name = (firstName == null? "":firstName + " " + lastName == null? "":lastName).trim();
        }
    }

    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

}