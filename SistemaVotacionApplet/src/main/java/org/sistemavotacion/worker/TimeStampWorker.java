package org.sistemavotacion.worker;

import static org.sistemavotacion.Contexto.*;

import java.util.Hashtable;
import java.util.List;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacionAppletFirma/master/licencia.txt
*/
public class TimeStampWorker extends SwingWorker<Respuesta, String> {
    
    private static Logger logger = LoggerFactory.getLogger(TimeStampWorker.class);

    private Integer id;
    private String urlArchivo;
    private WorkerListener workerListener;
    private TimeStampToken timeStampToken = null;
    private Attribute timeStampAsAttribute = null;
    private AttributeTable timeStampAsAttributeTable = null;
    private TimeStampRequest timeStampRequest = null;

    public TimeStampWorker(Integer id, String urlArchivo, 
            WorkerListener workerListener, byte[] digest, 
            String timeStampRequestAlg) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        timeStampRequest = reqgen.generate(timeStampRequestAlg, digest);
    }
    
    public TimeStampWorker(Integer id, String urlArchivo, 
            WorkerListener workerListener, TimeStampRequest timeStampRequest) {
        this.id = id;
        this.urlArchivo = urlArchivo;
        this.workerListener = workerListener;
        this.timeStampRequest = timeStampRequest;
    }
    
    public int getId(){
        return this.id;
    }

    @Override//on the EDT
    protected void done() {
        try {
            workerListener.showResult(this, get());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            workerListener.showResult(this, respuesta);
        }
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    @Override
    protected Respuesta doInBackground() throws Exception {
        Respuesta respuesta = null;
        try {
            HttpResponse response = Contexto.getInstancia().getHttpHelper().
                    enviarByteArray(timeStampRequest.getEncoded(), urlArchivo);
            respuesta = new Respuesta(response.getStatusLine().getStatusCode());
            if (Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
                byte[] bytesToken = EntityUtils.toByteArray(response.getEntity());
                timeStampToken = new TimeStampToken(
                        new CMSSignedData(bytesToken));
                DERObject derObject = new ASN1InputStream(
                        timeStampToken.getEncoded()).readObject();
                DERSet derset = new DERSet(derObject);
                timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.
                		id_aa_signatureTimeStampToken, derset);
                Hashtable hashTable = new Hashtable();
                hashTable.put(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, timeStampAsAttribute);
                timeStampAsAttributeTable = new AttributeTable(hashTable);
                respuesta.setBytesArchivo(bytesToken);
            } else respuesta.setMensaje(EntityUtils.toString(response.getEntity()));
            EntityUtils.consume(response.getEntity());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new Respuesta(500, getString("connectionErrorMsg"));
        }
        return respuesta;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
    
    public Attribute getTimeStampTokenAsAttribute() {
        return timeStampAsAttribute;
    }
    
    public AttributeTable getTimeStampTokenAsAttributeTable() {
        return timeStampAsAttributeTable;
    }
}