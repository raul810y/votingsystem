package org.votingsystem.accesscontrol.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;


/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Entity @Table(name="RepresentativeDocument")
public class RepresentativeDocument implements Serializable {

    private static Logger log = Logger.getLogger(RepresentativeDocument.class);

    private static final long serialVersionUID = 1L;

	public enum State {OK, CANCELLED, RENEWED}
    
    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name="state", nullable=false) private State state = State.OK;
    @OneToOne private MessageSMIME activationSMIME;
    @OneToOne private MessageSMIME cancellationSMIME;
	@Column(name="description", columnDefinition="TEXT" ) private String description;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userVS") private UserVS userVS;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCanceled", length=23) private Date dateCanceled;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="lastUpdated", length=23) private Date lastUpdated;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDateCanceled() {
		return dateCanceled;
	}

	public RepresentativeDocument setDateCanceled(Date dateCanceled) {
		this.dateCanceled = dateCanceled;
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

	public MessageSMIME getActivationSMIME() {
		return activationSMIME;
	}

	public void setActivationSMIME(MessageSMIME activationSMIME) {
		this.activationSMIME = activationSMIME;
	}

	public MessageSMIME getCancellationSMIME() {
		return cancellationSMIME;
	}

	public RepresentativeDocument setCancellationSMIME(MessageSMIME cancellationSMIME) {
		this.cancellationSMIME = cancellationSMIME;
        return this;
	}

	public State getState() {
		return state;
	}

	public RepresentativeDocument setState(State state) {
		this.state = state;
        return this;
	}

	public UserVS getUserVS() {
		return userVS;
	}

	public void setUserVS(UserVS user) {
		this.userVS = user;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
