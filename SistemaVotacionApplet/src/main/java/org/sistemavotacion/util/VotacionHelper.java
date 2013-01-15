package org.sistemavotacion.util;

import static org.sistemavotacion.Contexto.*;

import org.sistemavotacion.smime.*;
import java.awt.Frame;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.mail.Header;
import javax.swing.JFileChooser;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.smime.DNIeSignedMailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class VotacionHelper {

    private static Logger logger = LoggerFactory.getLogger(VotacionHelper.class);
    
    public static final String ASUNTO_MENSAJE_SOLICITUD_ACCESO = "[SOLICITUD ACCESO]-";     
    public static final String NOMBRE_DESTINATARIO = "Sistema de Votación"; 
    
    private static Map<String, ReciboVoto> mapaRecibos;
  
    public static Evento prepararVoto (Evento evento) throws NoSuchAlgorithmException {    
        evento.setOrigenHashSolicitudAcceso(UUID.randomUUID().toString());
        evento.setHashSolicitudAccesoBase64(CMSUtils.getHashBase64(
            evento.getOrigenHashSolicitudAcceso(), VOTING_DATA_DIGEST));
        evento.setOrigenHashCertificadoVoto(UUID.randomUUID().toString());
        evento.setHashCertificadoVotoBase64(CMSUtils.getHashBase64(
            evento.getOrigenHashCertificadoVoto(), VOTING_DATA_DIGEST));         
        return evento;
    }
   
    public static File obtenerSolicitudAcceso (Evento voto, 
            String password, File resultado) throws NoSuchAlgorithmException, Exception {
        String asuntoMensaje = 
                ASUNTO_MENSAJE_SOLICITUD_ACCESO + voto.getEventoId();
        File solicitudAcceso = DNIeSignedMailGenerator.genFile("",
                NOMBRE_DESTINATARIO, voto.obtenerSolicitudAccesoJSONStr(),
                password.toCharArray(), asuntoMensaje, resultado);
        return solicitudAcceso;
    }
    
    public static String obtenerAnuladorDeVotoJSONStr(String hashCertificadoVotoBase64) {
        logger.debug("obtenerAnuladorDeVotoJSONStr");
        JSONObject jsonObject = obtenerAnuladorDeVotoJSON(hashCertificadoVotoBase64);
        if(jsonObject == null) return null;
        return jsonObject.toString();
    }
    
    public static JSONObject obtenerAnuladorDeVotoJSON(String hashCertificadoVotoBase64) {
        logger.debug("obtenerAnuladorDeVotoJSON");
        if(mapaRecibos == null) return null;
        ReciboVoto recibo = mapaRecibos.get(hashCertificadoVotoBase64);
        if(recibo == null) return null;
        Evento voto = recibo.getVoto();
        Map map = new HashMap();
        map.put("origenHashCertificadoVoto", voto.getOrigenHashCertificadoVoto());
        map.put("hashCertificadoVotoBase64", voto.getHashCertificadoVotoBase64());
        map.put("origenHashSolicitudAcceso", voto.getOrigenHashSolicitudAcceso());
        map.put("hashSolicitudAccesoBase64", voto.getHashSolicitudAccesoBase64());
        return (JSONObject) JSONSerializer.toJSON(map);
    }
    
    public static void addRecibo(String hashCertificadoVotoBase64, ReciboVoto recibo) {
        if(mapaRecibos == null) mapaRecibos = new HashMap<String, ReciboVoto>();
        mapaRecibos.put(hashCertificadoVotoBase64, recibo);
    }
    
    public static ReciboVoto getReciboVoto(String hashCertificadoVotoBase64) {
        if(mapaRecibos == null || hashCertificadoVotoBase64 == null) return null;
        return mapaRecibos.get(hashCertificadoVotoBase64);
    }
    
    public static Operacion guardarRecibo(
            String hashCertificadoVotoBase64, Frame frame) {
        Operacion respuesta = new Operacion();
        frame.setLocationRelativeTo(null);
        ReciboVoto recibo = getReciboVoto(hashCertificadoVotoBase64);
        if(recibo != null)  respuesta = guardarRecibo(recibo, frame);
        else {
            respuesta.setCodigoEstado(Operacion.SC_ERROR_PETICION);
            respuesta.setMensaje("No se ha encontrado el recibo para " +
                        hashCertificadoVotoBase64);
        }
        return respuesta;
    }
    
       
    private static Operacion guardarRecibo(ReciboVoto reciboVoto, Frame frame) {
        String resultado = "Anulada operación";
        Operacion respuesta = new Operacion(Operacion.SC_CANCELADO);
        try {
            final JFileChooser chooser = new JFileChooser();
            File recibo = reciboVoto.getArchivoRecibo();
            chooser.setSelectedFile(recibo);
            int returnVal = chooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file.getName().indexOf(".") == -1) {
                    String fileName = file.getAbsolutePath();
                    file = new File(fileName);
                }
                if (file != null) {
                    recibo.renameTo(file);
                    resultado = "Guardado recibo en -> " + file.getAbsolutePath();
                    respuesta.setCodigoEstado(Operacion.SC_OK);
                }
            }
            recibo.delete();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        respuesta.setMensaje(resultado);
        logger.debug("- guardarRecibo - resultado: " + respuesta.obtenerJSONStr());
        return respuesta;
    }
    
}