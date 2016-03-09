package org.votingsystem.web.ejb;

import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.currency.SubscriptionVSDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.SubscriptionVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class SubscriptionVSBean {

    private static final Logger log = Logger.getLogger(SubscriptionVSBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject CMSBean cmsBean;

    public UserVS checkUser(UserVS userVS) throws Exception {
        log.log(Level.FINE, "nif: " + userVS.getNif());
        if(userVS.getNif() == null) throw new ExceptionVS("ERROR - missing Nif");
        X509Certificate x509Cert = userVS.getCertificate();
        if (x509Cert == null) throw new ExceptionVS("Missing certificate!!!");
        userVS.setNif(org.votingsystem.util.NifUtils.validate(userVS.getNif()));
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", userVS.getNif());
        UserVS userVSDB = dao.getSingleResult(UserVS.class, query);
        CertExtensionDto deviceData = CertUtils.getCertExtensionData(CertExtensionDto.class, x509Cert,
                ContextVS.DEVICEVS_OID);
        if (userVSDB == null) {
            userVSDB = dao.persist(userVS);
            config.createIBAN(userVS);
            log.log(Level.INFO, "checkUser ### NEW UserVS: " + userVSDB.getNif());
        } else {
            userVSDB.setCertificateCA(userVS.getCertificateCA());
            userVSDB.setCertificate(userVS.getCertificate());
            userVSDB.setTimeStampToken(userVS.getTimeStampToken());
        }
        setUserData(userVSDB, deviceData);
        return userVSDB;
    }

    public void setUserData(UserVS userVS, CertExtensionDto deviceData) throws
            CertificateException, IOException, NoSuchAlgorithmException, NoSuchProviderException {
        log.log(Level.FINE, " deviceData: " + deviceData);
        X509Certificate x509Cert = userVS.getCertificate();
        Query query = dao.getEM().createQuery("SELECT c FROM CertificateVS c WHERE c.userVS =:userVS and c.state =:state " +
                "and c.serialNumber =:serialNumber and c.authorityCertificateVS =:authorityCertificateVS")
                .setParameter("userVS", userVS).setParameter("state",CertificateVS.State.OK)
                .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                .setParameter("authorityCertificateVS", userVS.getCertificateCA());
        CertificateVS certificate = dao.getSingleResult(CertificateVS.class, query);
        DeviceVS deviceVS = null;
        if(certificate == null){
            certificate = dao.persist(CertificateVS.USER(userVS, x509Cert));
            if(deviceData != null) {
                query = dao.getEM().createNamedQuery("findDeviceByUserAndDeviceId").setParameter("userVS", userVS)
                        .setParameter("deviceId", deviceData.getDeviceId());
                deviceVS = dao.getSingleResult(DeviceVS.class, query);
                if(deviceVS == null) {
                    deviceVS = dao.persist(new DeviceVS(userVS, deviceData.getDeviceId(), deviceData.getEmail(),
                            deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate));
                    log.log(Level.FINE, "new device with id: " + deviceVS.getId());
                } else dao.getEM().merge(deviceVS.updateCertInfo(deviceData));
            }
            log.log(Level.FINE, "new certificate with id:" + certificate.getId());
        } else if(deviceData != null && deviceData.getDeviceId() != null) {
            query = dao.getEM().createQuery("SELECT d FROM DeviceVS d WHERE d.deviceId =:deviceId and d.certificateVS =:certificate")
                    .setParameter("deviceId", deviceData.getDeviceId())
                    .setParameter("certificate", certificate);
            deviceVS = dao.getSingleResult(DeviceVS.class, query);
            if(deviceVS == null) {
                deviceVS = dao.persist(new DeviceVS(userVS, deviceData.getDeviceId(), deviceData.getEmail(),
                        deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate));
                log.log(Level.FINE, "new device with id: " + deviceVS.getId());
            }
        }
        userVS.setCertificateVS(certificate);
        userVS.setDeviceVS(deviceVS);
    }

    @Transactional
    public DeviceVS checkDeviceFromCSR(DeviceVSDto dto) throws ExceptionVS {
        log.info(format("checkDevice - givenname: {0} - surname: {1} - nif:{2} - phone: {3}" +
                " - email: {4} - deviceId: {5} - deviceType: {6}", dto.getFirstName(), dto.getLastName(), dto.getNIF(),
                dto.getPhone(), dto.getEmail(), dto.getDeviceId(), dto.getDeviceType()));
        if(dto.getNIF() == null) throw new ValidationExceptionVS("missing 'nif'");
        if(dto.getDeviceId() == null) throw new ValidationExceptionVS("missing 'deviceId'");
        String validatedNIF = org.votingsystem.util.NifUtils.validate(dto.getNIF());
        Query query = dao.getEM().createQuery("select u from UserVS u where u.nif =:nif").setParameter("nif", validatedNIF);
        UserVS userVS = dao.getSingleResult(UserVS.class, query);
        if (userVS == null) userVS = dao.persist(new UserVS(validatedNIF, UserVS.Type.USER, null,
                dto.getFirstName(), dto.getLastName(), dto.getEmail(), dto.getPhone()));
        DeviceVS deviceVS = new DeviceVS(userVS, dto.getDeviceId(), dto.getEmail(), dto.getPhone(),
                dto.getDeviceType());
        deviceVS.setState(DeviceVS.State.PENDING);
        return dao.persist(deviceVS);
    }

    public SubscriptionVS deActivateUser(CMSMessage cmsMessage) throws Exception {
        UserVS signer = cmsMessage.getUserVS();
        log.log(Level.FINE, "signer: " + signer.getNif());
        SubscriptionVSDto request = cmsMessage.getSignedContent(SubscriptionVSDto.class);
        GroupVS groupVS = dao.find(GroupVS.class, request.getGroupvsId());
        if(groupVS == null || !request.getGroupvsName().equals(groupVS.getName())) {
            throw new ExceptionVS("group with name: " + request.getGroupvsName() + " and id: " + request.getId() + " not found");
        }
        if(!groupVS.getRepresentative().getNif().equals(request.getUserVSNIF()) && !cmsBean.isAdmin(signer.getNif())) {
            throw new ExceptionVS("'userWithoutGroupPrivilegesErrorMsg - groupVS:" + request.getGroupvsName() + " - nif:" +
                    signer.getNif());
        }
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getUserVSNIF());
        UserVS groupUser = dao.getSingleResult(UserVS.class, query);
        if(groupUser == null) throw new ValidationExceptionVS("user unknown - nif:" + request.getUserVSNIF());
        query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("groupVS", groupVS)
                .setParameter("userVS", groupUser);
        SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscription == null || SubscriptionVS.State.CANCELED == subscription.getState()) {
            throw new ExceptionVS("groupUserAlreadyCencelledErrorMsg - user nif: " + request.getUserVSNIF() +
                    " - group: " + request.getGroupvsName());
        }
        subscription.setReason(request.getReason());
        subscription.setState(SubscriptionVS.State.CANCELED);
        subscription.setDateCancelled(new Date());
        subscription.setCancellationCMS(cmsMessage);
        log.info("deActivateUser OK - user nif: " + request.getUserVSNIF() + " - group: " + request.getGroupvsName());
        return subscription;
    }

    public SubscriptionVS activateUser(CMSMessage cmsMessage) throws Exception {
        UserVS signer = cmsMessage.getUserVS();
        log.info("signer: " + signer.getNif());
        SubscriptionVSDto request = cmsMessage.getSignedContent(SubscriptionVSDto.class);
        request.validateActivationRequest();
        GroupVS groupVS = dao.find(GroupVS.class, request.getGroupvsId());
        if(groupVS == null || !request.getGroupvsName().equals(groupVS.getName())) {
            throw new ValidationExceptionVS("Group with id: " + request.getId() + " and name: " + request.getGroupvsName() + " not found");
        }
        if(!groupVS.getRepresentative().getNif().equals(signer.getNif()) && !cmsBean.isAdmin(signer.getNif())) {
            throw new ValidationExceptionVS("userWithoutGroupPrivilegesErrorMsg - operation: " +
                    TypeVS.CURRENCY_GROUP_USER_ACTIVATE.toString() + " - nif: " + signer.getNif() + " - group: " +
                    request.getGroupvsName());
        }
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", request.getUserVSNIF());
        UserVS groupUser = dao.getSingleResult(UserVS.class, query);
        if(groupUser == null) throw new ValidationExceptionVS("user unknown - nif:" + request.getUserVSNIF());
        query = dao.getEM().createNamedQuery("findSubscriptionByGroupAndUser").setParameter("groupVS", groupVS)
                .setParameter("userVS", groupUser);
        SubscriptionVS subscription = dao.getSingleResult(SubscriptionVS.class, query);
        if(subscription == null) throw new ValidationExceptionVS("user:" + request.getUserVSNIF() +
                " has not pending subscription request");
        subscription.setState(SubscriptionVS.State.ACTIVE);
        subscription.setDateActivated(new Date());
        subscription.setActivationCMS(cmsMessage);
        log.info("activateUser OK - user nif: " + request.getUserVSNIF() + " - group: " + request.getGroupvsName());
        return subscription;
    }
    
}
