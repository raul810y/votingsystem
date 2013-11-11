package org.votingsystem.accesscontrol.model;

import javax.persistence.GeneratedValue;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.votingsystem.model.TypeVS;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name="SolicitudCopia")
public class SolicitudCopia implements java.io.Serializable {

	 private static final long serialVersionUID = 1L;
	 
	 
	 @Id @GeneratedValue(strategy=IDENTITY)
	 @Column(name="id", unique=true, nullable=false)
	 private Long id;
	 @OneToOne
	 @JoinColumn(name="documentoId")
	 private Documento documento;
     @Enumerated(EnumType.STRING)
     @Column(name="type", nullable=false)
     private TypeVS type;
     @OneToOne
     @JoinColumn(name="messageSMIMEId")
     private MessageSMIME messageSMIME;
     @ManyToOne(fetch=FetchType.LAZY)
     @JoinColumn(name="representativeId")
     private Usuario representative;
     @Column(name="email") 
     private String email;
     @Column(name="filePath") 
     private String filePath;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="fechaCreacion", length=23)
     private Date dateCreated;
     @Temporal(TemporalType.TIMESTAMP)
     @Column(name="lastUpdated", length=23)
     private Date lastUpdated;
     
     public SolicitudCopia() { }


     public Long getId() {
		return id;
     }


	public void setId(Long id) {
		this.id = id;
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

	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public Documento getDocumento() {
		return documento;
	}


	public void setDocumento(Documento documento) {
		this.documento = documento;
	}

	public MessageSMIME getMessageSMIME() {
		return messageSMIME;
	}


	public void setMessageSMIME(MessageSMIME messageSMIME) {
		this.messageSMIME = messageSMIME;
	}


	public Usuario getRepresentative() {
		return representative;
	}


	public void setRepresentative(Usuario representative) {
		this.representative = representative;
	}


	public TypeVS getType() {
		return type;
	}


	public void setType(TypeVS type) {
		this.type = type;
	}

}