package org.votingsystem.model.currency;

import org.votingsystem.model.CMSMessage;
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
@Table(name="SubscriptionVS")
@NamedQueries({
        @NamedQuery(name = "findSubscriptionByGroupAndUser", query =
                "SELECT s FROM SubscriptionVS s WHERE s.userVS =:userVS AND s.groupVS =:groupVS"),
        @NamedQuery(name="countSubscriptionByGroupVSAndState", query=
                "SELECT COUNT(s) FROM SubscriptionVS s WHERE s.groupVS =:groupVS AND s.state =:state")
})
public class SubscriptionVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public CMSMessage getCancellationCMS() {
        return cancellationCMS;
    }

    public void setCancellationCMS(CMSMessage cancellationCMS) {
        this.cancellationCMS = cancellationCMS;
    }

    public CMSMessage getActivationCMS() {
        return activationCMS;
    }

    public void setActivationCMS(CMSMessage activationCMS) {
        this.activationCMS = activationCMS;
    }

    public enum State {ACTIVE, PENDING, CANCELED}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="groupVS") private GroupVS groupVS;

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Column(name="reason", columnDefinition="TEXT") private String reason;

    @OneToOne
    @JoinColumn(name="subscriptionCMS") private CMSMessage subscriptionCMS;

    @OneToOne
    @JoinColumn(name="activationCMS") private CMSMessage activationCMS;

    @OneToOne
    @JoinColumn(name="cancellationCMS") private CMSMessage cancellationCMS;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCancelled", length=23) private Date dateCancelled;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateActivated", length=23) private Date dateActivated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public SubscriptionVS() {}

    public SubscriptionVS(UserVS userVS, GroupVS groupVS, State state, CMSMessage cmsMessage) {
        this.userVS = userVS;
        this.groupVS = groupVS;
        this.state = state;
        this.subscriptionCMS = cmsMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public GroupVS getGroupVS() {
        return groupVS;
    }

    public void setGroupVS(GroupVS groupVS) {
        this.groupVS = groupVS;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CMSMessage getSubscriptionCMS() {
        return subscriptionCMS;
    }

    public void setSubscriptionCMS(CMSMessage subscriptionCMS) {
        this.subscriptionCMS = subscriptionCMS;
    }

    public Date getDateCancelled() {
        return dateCancelled;
    }

    public void setDateCancelled(Date dateCancelled) {
        this.dateCancelled = dateCancelled;
    }

    public Date getDateActivated() {
        return dateActivated;
    }

    public void setDateActivated(Date dateActivated) {
        this.dateActivated = dateActivated;
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

}