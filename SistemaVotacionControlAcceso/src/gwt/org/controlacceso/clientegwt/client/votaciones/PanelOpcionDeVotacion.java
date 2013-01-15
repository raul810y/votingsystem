package org.controlacceso.clientegwt.client.votaciones;

import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.modelo.OpcionDeEventoJso;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class PanelOpcionDeVotacion extends Composite {
	
    private static Logger logger = Logger.getLogger("PanelOpcionDeVotacion");

	private static PanelOpcionDeVotacionUiBinder uiBinder = GWT
			.create(PanelOpcionDeVotacionUiBinder.class);

	interface PanelOpcionDeVotacionUiBinder extends UiBinder<Widget, PanelOpcionDeVotacion> {	}

    @UiField Image borrarImage;
    @UiField Label contenidoLabel;
	OpcionDeEventoJso opcion;
	PanelPublicacionOpcionesVotacion panelOpciones;

	private boolean enabled = true;

	public PanelOpcionDeVotacion(final OpcionDeEventoJso opcion, 
			final PanelPublicacionOpcionesVotacion panelOpciones) {
		initWidget(uiBinder.createAndBindUi(this));
		borrarImage.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				borrarOpcion();
			}});
		contenidoLabel.setText(opcion.getContenido());
		this.opcion = opcion;
		this.panelOpciones = panelOpciones;
	}
    

	public OpcionDeEventoJso getOpcion() {
		return opcion;
	}

	private void borrarOpcion() {
		if(enabled) {
			if(Window.confirm(Constantes.INSTANCIA.confirmarBorradoOpcion())){
				panelOpciones.borrarOpcion(opcion);
			}
		}
	}

	public void setEnabled(boolean opcionesEnabled) {
		this.enabled  = opcionesEnabled;
		
	}
    


}