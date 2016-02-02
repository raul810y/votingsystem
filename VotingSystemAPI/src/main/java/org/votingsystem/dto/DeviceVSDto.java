package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CryptoTokenVS;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceVSDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deviceId;
    private String sessionId;
    private String deviceName;
    private String email;
    private String phone;
    private String certPEM;
    private String firstName;
    private String lastName;
    private String NIF;
    private String IBAN;
    private DeviceVS.Type deviceType;
    private CryptoTokenVS type;

    public DeviceVSDto() {}

    public DeviceVSDto(CryptoTokenVS type) {
        this.type = type;
    }

    public DeviceVSDto(UserVS userVS, CertExtensionDto certExtensionDto) {
        this.NIF = userVS.getNif();
        this.firstName = userVS.getFirstName();
        this.lastName = userVS.getLastName();
        this.phone = certExtensionDto.getMobilePhone();
        this.email = certExtensionDto.getEmail();
        this.deviceId = certExtensionDto.getDeviceId();
        this.deviceType = certExtensionDto.getDeviceType();
    }

    public DeviceVSDto(Long id, String name) {
        this.setId(id);
        this.setDeviceName(name);
    }

    public DeviceVSDto(Long id, String deviceId, String sessionId) {
        this.setId(id);
        this.setDeviceId(deviceId);
        this.setSessionId(sessionId);
    }

    public static DeviceVSDto INIT_AUTHENTICATED_SESSION(UserVS userVS) throws Exception {
        DeviceVSDto deviceVSDto = new DeviceVSDto(userVS.getDeviceVS());
        deviceVSDto.setIBAN(userVS.getIBAN());
        return deviceVSDto;
    }

    public DeviceVSDto(DeviceVS deviceVS) throws Exception {
        this.setId(deviceVS.getId());
        this.setDeviceId(deviceVS.getDeviceId());
        this.setDeviceName(deviceVS.getDeviceName());
        this.setPhone(deviceVS.getPhone());
        this.setEmail(deviceVS.getEmail());
        X509Certificate x509Cert = deviceVS.getX509Certificate();
        if(x509Cert != null) certPEM = new String(CertUtils.getPEMEncoded(x509Cert));
    }

    @JsonIgnore
    public DeviceVS getDeviceVS() throws Exception {
        DeviceVS deviceVS = new DeviceVS();
        deviceVS.setId(getId());
        deviceVS.setDeviceId(getDeviceId());
        deviceVS.setDeviceName(getDeviceName());
        deviceVS.setEmail(getEmail());
        deviceVS.setPhone(getPhone());
        if(getCertPEM() != null) {
            Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(getCertPEM().getBytes());
            deviceVS.setX509Certificate(certChain.iterator().next());
        }

        return deviceVS;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCertPEM() {
        return certPEM;
    }

    public void setCertPEM(String certPEM) {
        this.certPEM = certPEM;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public CryptoTokenVS getType() {
        return type;
    }

    public void setType(CryptoTokenVS type) {
        this.type = type;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNIF() {
        return NIF;
    }

    public void setNIF(String NIF) {
        this.NIF = NIF;
    }

    public DeviceVS.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceVS.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

}
