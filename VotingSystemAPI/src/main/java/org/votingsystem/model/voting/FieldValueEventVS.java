package org.votingsystem.model.voting;

import org.votingsystem.model.CMSMessage;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity @Table(name="FieldValueEventVS")
public class FieldValueEventVS extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne private CMSMessage cmsMessage;
    @Column(name="value", length=1000) private String value;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fieldEventVS", nullable=false) private FieldEventVS fieldEventVS;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
    
    public FieldValueEventVS() {}

    public FieldValueEventVS(CMSMessage cmsMessage, FieldEventVS fieldEventVS, String value) {
        this.cmsMessage = cmsMessage;
        this.fieldEventVS = fieldEventVS;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFieldEventVS(FieldEventVS fieldEventVS) {
        this.fieldEventVS = fieldEventVS;
    }

    public FieldEventVS getFieldEventVS() {
        return fieldEventVS;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public void setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }
    
}
