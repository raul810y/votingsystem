package org.controlacceso.clientegwt.client.votaciones;

import java.util.List;
import java.util.logging.Logger;

import org.controlacceso.clientegwt.client.Constantes;
import org.controlacceso.clientegwt.client.dialogo.ResultDialog;
import org.controlacceso.clientegwt.client.modelo.ConsultaEventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventoSistemaVotacionJso;
import org.controlacceso.clientegwt.client.modelo.EventosSistemaVotacionJso;
import org.controlacceso.clientegwt.client.panel.BarraNavegacion;
import org.controlacceso.clientegwt.client.panel.PanelEncabezado;
import org.controlacceso.clientegwt.client.panel.PanelEvento;
import org.controlacceso.clientegwt.client.util.RequestHelper;
import org.controlacceso.clientegwt.client.util.ServerPaths;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PanelVotaciones extends Composite implements BarraNavegacion.Listener {

    private static Logger logger = Logger.getLogger("PanelVotaciones");

  
    @UiField BarraNavegacion barraNavegacion;
    @UiField VerticalPanel panelEventos;
    @UiField VerticalPanel panelBarrarProgreso;
    @UiField FlowPanel panelContenedorEventos;
    @UiField HTML emptySearchLabel;
 

    private static PanelVotacionesUiBinder uiBinder = GWT.create(PanelVotacionesUiBinder.class);

    interface PanelVotacionesUiBinder extends UiBinder<VerticalPanel, PanelVotaciones> { }

    public PanelVotaciones() {
    	initWidget(uiBinder.createAndBindUi(this));
    	panelContenedorEventos.clear();
        panelEventos.setVisible(false);
        emptySearchLabel.setVisible(false);
    }

    private class ServerRequestCallback implements RequestCallback {

        @Override
        public void onError(Request request, Throwable exception) {
        	showErrorDialog (Constantes.INSTANCIA.exceptionLbl(), 
        			exception.getMessage());                
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            if (response.getStatusCode() == Response.SC_OK) {
                ConsultaEventosSistemaVotacionJso consulta = 
                		ConsultaEventosSistemaVotacionJso.create(response.getText());
                recepcionConsultaEventos(consulta);
            } else {
            	if(response.getStatusCode() == 0) {//Magic Number!!! -> network problem
            		showErrorDialog (Constantes.INSTANCIA.errorLbl() , 
            				Constantes.INSTANCIA.networkERROR());
            	} else showErrorDialog (String.valueOf(
            			response.getStatusCode()), response.getText());
            }
        }

    }
    
    private void showErrorDialog (String caption, String message) {
    	ResultDialog resultDialog = new ResultDialog();
		resultDialog.show(caption, message,Boolean.FALSE);  
    }

	public void recepcionConsultaEventos(ConsultaEventosSistemaVotacionJso consulta) {
		logger.info("recepcionConsultaEventos - ");
        barraNavegacion.addListener(this, consulta.getOffset(), Constantes.EVENTS_RANGE, 
        		 consulta.getNumeroTotalEventosVotacionEnSistema());
	    EventosSistemaVotacionJso eventos = consulta.getEventos();
	    List<EventoSistemaVotacionJso> votaciones;
	    panelContenedorEventos.clear();
		if (eventos != null && (votaciones = eventos.getVotacionesList())!= null &&
				eventos.getVotacionesList().size() > 0) {
			emptySearchLabel.setVisible(false);
			for(EventoSistemaVotacionJso votacion: votaciones) {
				PanelEvento panelEvento = new PanelEvento(votacion);
				panelContenedorEventos.add(panelEvento);
			}
	    } else {
	        emptySearchLabel.setVisible(true);
	    	barraNavegacion.setVisible(false);
	    }
		panelBarrarProgreso.setVisible(false);
		panelEventos.setVisible(true);
	}

	@Override
	public void gotoPage(int offset, int range) {
		logger.info("--- gotoPage ---");
		RequestHelper.doGet(ServerPaths.getUrlEventosVotacion(range, offset, 
				PanelEncabezado.INSTANCIA.getEstadoEvento()), new ServerRequestCallback());
		panelBarrarProgreso.setVisible(true);
		panelEventos.setVisible(false);
	}

}