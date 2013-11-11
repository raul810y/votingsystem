package org.votingsystem.accesscontrol.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenizerDef;
import org.votingsystem.model.EventVS;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
@Entity@Indexed
@Table(name="Evento")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="tipoDeEvento",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue("Evento")
@AnalyzerDef(name="customAnalyzer",
	charFilters = { @CharFilterDef(factory = HTMLStripCharFilterFactory.class) }, 
	tokenizer =  @TokenizerDef(factory = StandardTokenizerFactory.class)
)
public class Evento implements Serializable, EventVS {

    private static final long serialVersionUID = 1L;

    public enum Estado {ACTIVO, FINALIZADO, CANCELADO, ACTORES_PENDIENTES_NOTIFICACION, PENDIENTE_COMIENZO,
    	PENDIENTE_DE_FIRMA, BORRADO_DE_SISTEMA}
    
    public enum Cardinalidad { MULTIPLES, UNA}

    @Id @GeneratedValue(strategy=IDENTITY) 
    @Column(name="id", unique=true, nullable=false)
    @DocumentId
    private Long id;
    @Column(name="contenido", columnDefinition="TEXT")
    @Analyzer(definition = "customAnalyzer")
    private String contenido; 
    @Column(name="metaInf", columnDefinition="TEXT")
    private String metaInf = "{}"; 
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="centroControlId")
    private CentroControl centroControl;    
    @Column(name="asunto", length=1000)
    @Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)    
    private String asunto;
    @Enumerated(EnumType.STRING)
    @Column(name="estado")
    @Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)    
    private Estado estado;
    @OneToOne(mappedBy="evento")
    private AlmacenClaves almacenClaves;   
    @Lob @Column(name="pdf")
    private byte[] pdf;    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="usuarioId")
    private Usuario usuario;
    @Column(name="copiaSeguridadDisponible")
    private Boolean copiaSeguridadDisponible = Boolean.TRUE;
    //Owning Entity side of the relationship
    @JoinTable(name="EtiquetaEvento", 
        joinColumns = { 
            @JoinColumn(name = "eventoId", referencedColumnName = "id")
     	}, 
     	inverseJoinColumns = { 
            @JoinColumn(name = "etiquetaId", referencedColumnName = "id")
     	})
    @ManyToMany
    private Set<Etiqueta> etiquetaSet;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaInicio", length=23, nullable=false)
    @Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
    private Date fechaInicio;    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaFin", length=23, nullable=false)
    @Field(index = Index.NO, analyze=Analyze.NO, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
    private Date fechaFin;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCancelacion", length=23)
    private Date dateCanceled;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaCreacion", length=23)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="fechaActualizacion", length=23)
    private Date lastUpdated;

    public String getContenido () {
        return contenido;
    }
    
    public void setContenido (String contenido) {
        this.contenido = contenido;
    }

    public String getAsunto () {
        return asunto;
    }

    public String getSubject() {
    	return asunto;
    }
    
    public void setAsunto (String asunto) {
        this.asunto = asunto;
    }

        /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }


    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Date getFechaFin() {
        return fechaFin;
    }
    
    /**
     * @return the estado
     */
    public Estado getEstado() {
        return estado;
    }

    /**
     * @param estado the estado to set
     */
    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    /**
     * @return the centroControl
     */
    public CentroControl getCentroControl() {
        return centroControl;
    }

    /**
     * @param centroControl the centroControl to set
     */
    public void setCentroControl(CentroControl centroControl) {
        this.centroControl = centroControl;
    }

	public AlmacenClaves getAlmacenClaves() {
		return almacenClaves;
	}

	public void setAlmacenClaves(AlmacenClaves almacenClaves) {
		this.almacenClaves = almacenClaves;
	}
        
	public boolean isOpen(Date selectedDate) {
		if(selectedDate == null) return false;
		boolean result = false;
		if (selectedDate.after(fechaInicio) && selectedDate.before(fechaFin)) result = true;
		if(estado != null && (estado == Estado.CANCELADO || 
				estado == Estado.BORRADO_DE_SISTEMA)) result = false;
		return result;
	}
	
	public boolean isActiveDate(Date date) {
		boolean result = false;
		if (date.after(fechaInicio) && date.before(getDateFinish())) result = true;
		return result;
	}

	public byte[] getPdf() {
		return pdf;
	}

	public void setPdf(byte[] pdf) {
		this.pdf = pdf;
	}

	public Boolean getCopiaSeguridadDisponible() {
		return copiaSeguridadDisponible;
	}

	public void setCopiaSeguridadDisponible(Boolean copiaSeguridadDisponible) {
		this.copiaSeguridadDisponible = copiaSeguridadDisponible;
	}

	public Set<Etiqueta> getEtiquetaSet() {
		return etiquetaSet;
	}

	public void setEtiquetaSet(Set<Etiqueta> etiquetaSet) {
		this.etiquetaSet = etiquetaSet;
	}

	public Date getDateCanceled() {
		return dateCanceled;
	}

	public void setDateCanceled(Date dateCanceled) {
		this.dateCanceled = dateCanceled;
	}

	public String getMetaInf() {
		return metaInf;
	}

	public void setMetaInf(String metaInf) {
		this.metaInf = metaInf;
	}
	
	public Date getDateFinish() {
		if(dateCanceled != null) return dateCanceled;
		else return fechaFin;
	}

}