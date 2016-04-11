package org.votingsystem.web.currency.ejb;

import org.iban4j.Iban;
import org.votingsystem.dto.currency.BankDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Bank;
import org.votingsystem.model.currency.BankInfo;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SubscriptionBean;
import org.votingsystem.web.util.ConfigVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class BankBean {

    private static final Logger log = Logger.getLogger(BankBean.class.getName());


    @Inject DAOBean dao;
    @Inject BankBean bankBean;
    @Inject UserBean userBean;
    @Inject TransactionBean transactionBean;
    @Inject SubscriptionBean subscriptionBean;
    @Inject CMSBean cmsBean;
    @Inject ConfigVS config;

    public Bank saveBank(CMSMessage cmsReq) throws Exception {
        User signer = cmsReq.getUser();
        log.log(Level.FINE, "signer:" + signer.getNif());
        BankDto request = cmsReq.getSignedContent(BankDto.class);
        request.validatePublishRequest();
        Iban IBAN = Iban.valueOf(request.getIBAN());
        if(!cmsBean.isAdmin(signer.getNif())) {
            throw new ValidationException("operation: " + request.getOperation() +
                    " - userWithoutPrivilegesErrorMsg - nif: " + signer.getNif());
        }
        Collection<X509Certificate> certChain = PEMUtils.fromPEMToX509CertCollection(request.getCertChainPEM().getBytes());
        X509Certificate x509Certificate = certChain.iterator().next();
        Bank bank = Bank.getUser(x509Certificate);
        cmsBean.verifyUserCertificate(bank);
        String validatedNIF = org.votingsystem.util.NifUtils.validate(bank.getNif());
        Query query = dao.getEM().createNamedQuery("findUserByNIF").setParameter("nif", validatedNIF);
        Bank bankDB = dao.getSingleResult(Bank.class, query);
        if(bankDB == null) {
            bank.setDescription(request.getInfo()).setIBAN(request.getIBAN());
            bankDB = dao.persist(bank);
            dao.persist(new BankInfo(bankDB, IBAN.getBankCode()));
            log.info("NEW bank id: " + bankDB.getId());
        } else {
            bankDB.setDescription(request.getInfo()).setCertificateCA(bank.getCertificateCA());
            bankDB.setX509Certificate(bank.getX509Certificate());
            bankDB.setTimeStampToken(bank.getTimeStampToken());
        }
        bank = bankDB;
        subscriptionBean.setUserData(bank, null);
        config.createIBAN(bank);
        log.info("saveBank - Bank id: " + bank.getId() + " - " + x509Certificate.getSubjectDN().toString());
        return bank;
    }

    public void refreshBankInfoData() {
        List<Bank> bankList = dao.findAll(Bank.class);
        for(Bank bank : bankList) {
            Query query = dao.getEM().createNamedQuery("findBankInfoByBank").setParameter("bank", bank);
            BankInfo bankInfo = dao.getSingleResult(BankInfo.class, query);
            Iban iban = Iban.valueOf(bank.getIBAN());
            if(bankInfo != null) {
                dao.getEM().merge(bankInfo.setBankCode(iban.getBankCode()));
            } else dao.persist(new BankInfo(bank, iban.getBankCode()));
        }
    }

}
