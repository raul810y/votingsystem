package org.votingsystem.model;

import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.x509.extension.X509ExtensionUtil;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.ExceptionVS;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketBatch {

    private Map<String, Vicket> vicketsMap;
    private VicketServer vicketServer;
    private BigDecimal requestAmount;
    private BigDecimal vicketsValue;
    private String currencyCode;
    private List<Map> vicketCSRList;

    public VicketBatch(BigDecimal requestAmount, BigDecimal vicketsValue,
               String currencyCode, VicketServer vicketServer) throws Exception {
        this.setRequestAmount(requestAmount);
        this.setVicketServer(vicketServer);
        this.setCurrencyCode(currencyCode);
        this.vicketsValue = vicketsValue;
        this.vicketsMap = getVicketBatch(requestAmount,vicketsValue, currencyCode, vicketServer);
        vicketCSRList = new ArrayList<Map>();
        for(Vicket vicket : vicketsMap.values()) {
            Map csrVicketMap = new HashMap();
            csrVicketMap.put("currency", currencyCode);
            csrVicketMap.put("vicketValue", vicketsValue.toString());
            csrVicketMap.put("csr", new String(vicket.getCertificationRequest().getCsrPEM(), "UTF-8"));
            vicketCSRList.add(csrVicketMap);
        }
    }

    public Map<String, Vicket> getVicketsMap() {
        return vicketsMap;
    }

    public void setVicketsMap(Map<String, Vicket> vicketsMap) {
        this.vicketsMap = vicketsMap;
    }

    public VicketServer getVicketServer() {
        return vicketServer;
    }

    public void setVicketServer(VicketServer vicketServer) {
        this.vicketServer = vicketServer;
    }

    public BigDecimal getRequestAmount() {
        return requestAmount;
    }

    public void setRequestAmount(BigDecimal requestAmount) {
        this.requestAmount = requestAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getVicketsValue() {
        return vicketsValue;
    }

    public void setVicketsValue(BigDecimal vicketsValue) {
        this.vicketsValue = vicketsValue;
    }

    public static Map<String, Vicket> getVicketBatch(BigDecimal requestAmount,
             BigDecimal vicketsValue, String currencyCode, VicketServer vicketServer) {
        Map<String, Vicket> vicketsMap = new HashMap<String, Vicket>();
        BigDecimal numVickets = requestAmount.divide(vicketsValue);
        List<Vicket> vicketList = new ArrayList<Vicket>();
        for(int i = 0; i < numVickets.intValue(); i++) {
            Vicket vicket = new Vicket(vicketServer.getServerURL(),
                    vicketsValue, currencyCode, TypeVS.VICKET);
            vicketList.add(vicket);
            vicketsMap.put(vicket.getHashCertVSBase64(), vicket);
        }
        return vicketsMap;
    }

    public JSONObject getRequestJSON() {
        Map requestVicketMap = new HashMap();
        requestVicketMap.put("numVickets", vicketsMap.values().size());
        requestVicketMap.put("vicketValue", vicketsValue.intValue());

        List vicketsMapList = new ArrayList();
        vicketsMapList.add(requestVicketMap);

        Map smimeContentMap = new HashMap();
        smimeContentMap.put("totalAmount", requestAmount.toString());
        smimeContentMap.put("currency", currencyCode);
        smimeContentMap.put("vickets", vicketsMapList);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        smimeContentMap.put("serverURL", vicketServer.getServerURL());
        smimeContentMap.put("operation", TypeVS.VICKET_REQUEST.toString());
        JSONObject requestJSON = new JSONObject(smimeContentMap);
        return requestJSON;
    }

    public List<Map> getVicketCSRList () {
        return vicketCSRList;
    }

    public JSONObject getVicketCSRRequest() {
        Map csrRequestMap = new HashMap();
        csrRequestMap.put("vicketCSR", vicketCSRList);
        return new JSONObject(csrRequestMap);
    }

    public Vicket initVicket(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS(
                "Unable to init Vicket. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        byte[] vicketExtensionValue = x509Certificate.getExtensionValue(ContextVS.VICKET_OID);
        DERTaggedObject vicketCertDataDER = (DERTaggedObject)
                X509ExtensionUtil.fromExtensionValue(vicketExtensionValue);
        JSONObject vicketCertData = new JSONObject(((DERUTF8String)
                vicketCertDataDER.getObject()).toString());
        String hashCertVS = vicketCertData.getString("hashCertVS");
        Vicket vicket = vicketsMap.get(hashCertVS);
        vicket.setState(Vicket.State.OK);
        vicket.getCertificationRequest().initSigner(signedCsr.getBytes());
        return vicket;
    }

}
