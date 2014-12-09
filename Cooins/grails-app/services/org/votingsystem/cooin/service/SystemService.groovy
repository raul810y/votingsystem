package org.votingsystem.cooin.service

import grails.transaction.Transactional
import net.sf.json.JSONArray
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.FileUtils
import org.votingsystem.util.NifUtils
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.cooin.util.IbanVSUtil

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class SystemService {

    private UserVS systemUser
    private TagVS wildTag
    private Locale defaultLocale
    def grailsApplication
    def subscriptionVSService
    def signatureVSService
    def messageSource

    @Transactional
    public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(nif:grailsApplication.config.vs.systemNIF, type:UserVS.Type.SYSTEM,
                    name:grailsApplication.config.vs.serverName).save()
            systemUser.setIBAN(IbanVSUtil.getInstance().getIBAN(systemUser.id))
            systemUser.save()
            wildTag = new TagVS(name:TagVS.WILDTAG).save()
            String[] defaultTags = grailsApplication.mainContext.getResource(
                    grailsApplication.config.vs.defaulTagsFilePath).getFile()?.text.split(",")
            for(String tag: defaultTags) {
                TagVS newTagVS = new TagVS(name:tag).save()
                new CooinAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:systemUser,
                        balance:BigDecimal.ZERO, IBAN:systemUser.getIBAN(), tag:newTagVS).save()
            }
            new CooinAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:systemUser,
                    balance:BigDecimal.ZERO, IBAN:systemUser.getIBAN(), tag:wildTag).save()
        }
        updateAdmins()
        return [systemUser:systemUser]
    }

    @Transactional
    public JSONArray updateAdmins() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        File directory = grailsApplication.mainContext.getResource(
                grailsApplication.config.vs.certAuthoritiesDirPath).getFile()
        File[] adminCerts = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return fileName.startsWith("ADMIN_") && fileName.endsWith(".pem");
            }
        });
        JSONArray adminsArray = new JSONArray()
        for(File adminCert:adminCerts) {
            log.debug("$methodName - checking admin cert '${adminCert.absolutePath}'")
            X509Certificate x509AdminCert = CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromFile(adminCert))
            UserVS userVS = UserVS.getUserVS(x509AdminCert)
            signatureVSService.verifyUserCertificate(userVS)
            ResponseVS responseVS = subscriptionVSService.checkUser(userVS)
            if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS("Problems updating admin cert " +
                    "'${adminCert.absolutePath}'")
            userVS = responseVS.userVS
            if(!userVS.IBAN) subscriptionVSService.checkUserVSAccount(userVS)
            adminsArray.add(userVS.getNif())
        }
        getSystemUser().getMetaInfJSON().put("adminsDNI", adminsArray)
        getSystemUser().setMetaInf(getSystemUser().getMetaInfJSON().toString())
        getSystemUser().save()
        log.debug("$methodName - admins list: '${getSystemUser().getMetaInfJSON().adminsDNI}'")
        return systemUser.getMetaInfJSON().adminsDNI
    }

    private Map genBalanceForSystem(DateUtils.TimePeriod timePeriod) {
        String methodName = new Object() {}.getClass().getEnclosingMethod()?.getName();
        log.debug("$methodName - timePeriod [${timePeriod.toString()}]")
        Map resultMap = [timePeriod:timePeriod.getMap()]
        //to avoid circular references
        resultMap.userVS = ((UserVSService)grailsApplication.mainContext.getBean("userVSService")).getUserVSDataMap(
                getSystemUser(), false)
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            or {
                and {
                    eq('state', TransactionVS.State.OK)
                    isNotNull('transactionParent')
                    between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
                    //not{ inList("type", [TransactionVS.Type.COOIN_INIT_PERIOD]) }
                }
                and {
                    eq('state', TransactionVS.State.OK)
                    isNull('transactionParent')
                    inList("type", [TransactionVS.Type.COOIN_SEND])
                }
            }
        }
        TransactionVSService transactionVSService = (TransactionVSService)grailsApplication.mainContext.getBean(
                "transactionVSService")
        Map transactionListWithBalances = transactionVSService.getTransactionListWithBalances(
                transactionList, TransactionVS.Source.FROM)
        resultMap.transactionFromList = transactionListWithBalances.transactionList
        resultMap.balancesFrom = transactionListWithBalances.balances
        transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            isNull('transactionParent')
            between("dateCreated", timePeriod.getDateFrom(), timePeriod.getDateTo())
            not{ inList("type", [TransactionVS.Type.COOIN_SEND])}
        }
        transactionListWithBalances = transactionVSService.getTransactionListWithBalances(
                transactionList, TransactionVS.Source.FROM)
        resultMap.transactionToList = transactionListWithBalances.transactionList
        resultMap.balancesTo = transactionListWithBalances.balances
        return resultMap
    }

    public String getTagMessage(String tag) {
        if(TagVS.WILDTAG.equals(tag)) {
            return  messageSource.getMessage('wildTagMsg', null, locale)
        } else return  messageSource.getMessage('tagMsg', [tag].toArray(), locale)
    }

    boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        return getSystemUser().getMetaInfJSON()?.adminsDNI?.contains(nif)
    }

    public void updateTagBalance(BigDecimal amount, String currencyCode, TagVS tag) {
        CooinAccount tagAccount = CooinAccount.findWhere(userVS:getSystemUser(), tag:tag, currencyCode:currencyCode,
                state: CooinAccount.State.ACTIVE)
        if(!tagAccount) throw new Exception("THERE'S NOT ACTIVE SYSTEM ACCOUNT FOR TAG '${tag.name}' and currency '${currencyCode}'")
        tagAccount.balance = tagAccount.balance.add(amount)
        tagAccount.save()
    }

    public UserVS getSystemUser() {
        if(!systemUser) systemUser = init().systemUser
        return systemUser;
    }

    public TagVS getWildTag() {
        if(!wildTag) wildTag = TagVS.findWhere(name:TagVS.WILDTAG)
        return wildTag
    }

}