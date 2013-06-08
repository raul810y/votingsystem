package org.sistemavotacion.test.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sistemavotacion.worker.AccessRequestWorker;
import org.sistemavotacion.worker.SMIMESignedSenderWorker;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Voter implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(Voter.class);
   
    private Evento evento;
    private String nifFrom;
    private Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
        
    public Voter (Evento evento) throws Exception {
        this.evento = evento; 
        nifFrom = evento.getUsuario().getNif();
        respuesta = new Respuesta(Respuesta.SC_ERROR, evento);
    }
    
    @Override public Respuesta call() {
        try {
            KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nifFrom);
            String msgSubject = ContextoPruebas.INSTANCE.getString(
                    "accessRequestMsgSubject", evento.getEventoId());

            /*String cancelVoteStr =  evento.getCancelVoteJSON().toString();
            File anuladorVoto = new File(Contexto.getUserDirPath(nifFrom,
                    ContextoPruebas.DEFAULTS.APPDIR)
                    + Contexto.CANCEL_VOTE_FILE + evento.getEventoId() + 
                    "_usu" + nifFrom + ".json");
            FileUtils.copyStreamToFile(new ByteArrayInputStream(
                    cancelVoteStr.getBytes()), anuladorVoto);*/
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.DNIe_SIGN_MECHANISM);
            
            String accessRequestStr = evento.getAccessRequestJSON().toString();
            SMIMEMessageWrapper smimeMessage = signedMailGenerator.
                    genMimeMessage(nifFrom, 
                    evento.getControlAcceso().getNombreNormalizado(), 
                    accessRequestStr, msgSubject, null);
            evento.setUrlSolicitudAcceso(ContextoPruebas.INSTANCE.
                    getURLAccessRequest());
            
            X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                        getAccessControl().getCertificate();            
            AccessRequestWorker accessWorker = new AccessRequestWorker(null, 
                    smimeMessage, evento, destinationCert, null);
            accessWorker.execute();
            respuesta = accessWorker.get();
            if(Respuesta.SC_OK == accessWorker.getStatusCode()) {
                PKCS10WrapperClient wrapperClient = accessWorker.
                    getPKCS10WrapperClient();
                String votoJSON = evento.getVoteJSON().toString();
                String subject = ContextoPruebas.INSTANCE.getString(
                        "voteMsgSubject", evento.getEventoId());
                smimeMessage = wrapperClient.genMimeMessage(
                        evento.getHashCertificadoVotoBase64(), 
                        evento.getControlAcceso().getNombreNormalizado(),
                        votoJSON, subject, null);
                String urlVoteService = ContextoPruebas.getURLVoto(
                    evento.getCentroControl().getServerURL()); 
                SMIMESignedSenderWorker senderWorker = new SMIMESignedSenderWorker(
                        null, smimeMessage, urlVoteService, wrapperClient.
                        getKeyPair(), null, null);
                senderWorker.execute();
                respuesta = senderWorker.get();
                if (Respuesta.SC_OK == senderWorker.getStatusCode()) {  
                    SMIMEMessageWrapper validatedVote = respuesta.getSmimeMessage();
                    ReciboVoto reciboVoto = new ReciboVoto(
                                Respuesta.SC_OK, validatedVote, evento);
                    respuesta.setEvento(evento);
                    respuesta.setReciboVoto(reciboVoto);
                }
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        }
        return respuesta;
    }
    
}