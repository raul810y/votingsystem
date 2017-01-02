package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.Device;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertExtensionDto {

    private String deviceId;
    private String deviceName;
    private String email;
    private String mobilePhone;
    private String nif;
    private String givenname;
    private String surname;
    private Device.Type deviceType;


    public CertExtensionDto() {}

    public CertExtensionDto(String deviceId, String deviceName, String email, String phone, Device.Type deviceType) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.email = email;
        this.mobilePhone = phone;
        this.deviceType = deviceType;
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

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public Device.Type getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Device.Type deviceType) {
        this.deviceType = deviceType;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @JsonIgnore
    public String getPrincipal() {
        return "SERIALNUMBER=" + nif + ", GIVENNAME=" + givenname + ", SURNAME=" + surname;
    }
}
