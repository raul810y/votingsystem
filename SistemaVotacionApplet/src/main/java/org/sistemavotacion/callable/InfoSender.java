package org.sistemavotacion.callable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class InfoSender implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(InfoSender.class);

    private Integer id = null;
    private String urlToSendDocument;
    private Object documentoEnviado;
    private String documentContentType = null;
    private List<String> headerNameList = new ArrayList<String>();

    public InfoSender(Integer id, Object documentoEnviado, 
            String documentContentType, String urlToSendDocument, 
            String... headerNames) {
        if(headerNames != null) {
            for(String headerName: headerNames) {
                headerNameList.add(headerName);
            }
        }
        this.id = id;
        this.documentoEnviado = documentoEnviado;
        this.documentContentType = documentContentType;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    public InfoSender setDocumentoEnviado(Object documentoEnviado,
            String documentContentType) {
        this.documentoEnviado = documentoEnviado;
        this.documentContentType = documentContentType;
        return this;
    }
    
    @Override public Respuesta call() { 
        logger.debug("doInBackground - urlToSendDocument: " + urlToSendDocument);
        Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
        try {
            if(documentoEnviado instanceof File) {
                respuesta = Contexto.INSTANCE.getHttpHelper().sendFile((File)documentoEnviado, 
                    documentContentType, urlToSendDocument,
                    headerNameList.toArray(new String[headerNameList.size()]));
            } else if(documentoEnviado instanceof byte[]) {
                respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                        (byte[])documentoEnviado, documentContentType, urlToSendDocument, 
                        headerNameList.toArray(new String[headerNameList.size()]));
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        } finally {
            respuesta.setId(id);
            return respuesta;
        }
    }

}