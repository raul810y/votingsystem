package org.votingsystem.model;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

public class TagVSEventVS_PK implements Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne
    @JoinColumn(name = "tag", referencedColumnName = "ID")
    private TagVS tagVS;

    @ManyToOne
    @JoinColumn(name = "event", referencedColumnName = "ID")
    private EventVS eventVS;
}