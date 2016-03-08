package org.votingsystem.model.voting;

import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity
@Table(name="AnonymousDelegation")
public class AnonymousDelegation extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {OK, PENDING, FINISHED, CANCELED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private Status status;


    @Column(name="originHashAnonymousDelegation", unique=true) private String originHashAnonymousDelegation;
    @Column(name="hashAnonymousDelegation", unique=true) private String hashAnonymousDelegation;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @OneToOne private MessageCMS delegationCMS;

    @OneToOne private MessageCMS cancellationCMS;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateFrom", length=23, nullable=false)
    private Date dateFrom;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateTo", length=23, nullable=false)
    private Date dateTo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCancelled", length=23)
    private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23, insertable=true)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23, insertable=true)
    private Date lastUpdated;

    public AnonymousDelegation() {}

    public AnonymousDelegation(Status status, MessageCMS delegationCMS, UserVS userVS, Date dateFrom,
                               Date dateTo) {
        this.status = status;
        this.delegationCMS = delegationCMS;
        this.userVS = userVS;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}

    public Status getStatus() {
        return status;
    }

    public AnonymousDelegation setStatus(Status status) {
        this.status = status;
        return this;
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

    public MessageCMS getDelegationCMS() {
        return delegationCMS;
    }

    public void setDelegationCMS(MessageCMS delegationCMS) {
        this.delegationCMS = delegationCMS;
    }

    public MessageCMS getCancellationCMS() {
        return cancellationCMS;
    }

    public AnonymousDelegation setCancellationCMS(MessageCMS cancellationCMS) {
        this.cancellationCMS = cancellationCMS;
        return this;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public AnonymousDelegation setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
        return this;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public String getOriginHashAnonymousDelegation() {
        return originHashAnonymousDelegation;
    }

    public AnonymousDelegation setOriginHashAnonymousDelegation(String originHashAnonymousDelegation) {
        this.originHashAnonymousDelegation = originHashAnonymousDelegation;
        return this;
    }

    public String getHashAnonymousDelegation() {
        return hashAnonymousDelegation;
    }

    public void setHashAnonymousDelegation(String hashAnonymousDelegation) {
        this.hashAnonymousDelegation = hashAnonymousDelegation;
    }

}