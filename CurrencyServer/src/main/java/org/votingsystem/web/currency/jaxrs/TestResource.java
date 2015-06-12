package org.votingsystem.web.currency.jaxrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.votingsystem.dto.currency.BalancesDto;
import org.votingsystem.dto.currency.IncomesDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.MapUtils;
import org.votingsystem.web.currency.ejb.AuditBean;
import org.votingsystem.web.currency.ejb.BalancesBean;
import org.votingsystem.web.currency.util.LoggerVS;
import org.votingsystem.web.currency.websocket.SessionVSManager;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Path("/test")
public class TestResource {

    private static final Logger log = Logger.getLogger(TestResource.class.getSimpleName());

    @Inject AuditBean auditBean;
    @Inject BalancesBean balanceBean;
    @Inject DAOBean dao;
    @Inject ConfigVS config;


    private static ExecutorService executorService;

    static {
        executorService = Executors.newFixedThreadPool(5);
    }

    class EventBusListener {
        @Subscribe public void newUserVS(UserVS userVS) {
            log.info("newUserVS: " + userVS.getNif());
        }
    }

    @GET @Path("/test")
    public Response test(@Context ServletContext context, @Context HttpServletRequest req,
            @Context HttpServletResponse resp) throws JsonProcessingException, ValidationExceptionVS {
        Query query = dao.getEM().createQuery("SELECT b FROM BatchVS b WHERE b.content like :operation")
                .setParameter("operation",  "%CURRENCY_CHANGE%");

        List<CurrencyBatch> resultList = query.getResultList();
        for(CurrencyBatch currencyBatch : resultList) {
            currencyBatch.setType(TypeVS.CURRENCY_CHANGE);
            dao.merge(currencyBatch);
        }
        return Response.ok().entity("Updated " + resultList.size() + " messages").build();
    }

    @GET @Path("/testQuery")
    public Response testQuery(@Context ServletContext context, @Context HttpServletRequest req,
                             @Context HttpServletResponse resp) throws JsonProcessingException, ValidationExceptionVS {
        Query query = dao.getEM().createQuery("select SUM(c.balance), tag, c.currencyCode from CurrencyAccount c JOIN c.tag tag where c.state =:state " +
                "group by tag, c.currencyCode").setParameter("state", CurrencyAccount.State.ACTIVE);
         List<Object[]> resultList = query.getResultList();
        for(Object[] result : resultList) {
            log.info("" + result[0] + ((TagVS)result[1]).getName() + result[2]);
        }

        return Response.ok().entity("OK").build();
    }

    @POST @Path("/testPost")
    public Response testPost(@Context ServletContext context, @Context HttpServletRequest req, byte[] postData,
                         @Context HttpServletResponse resp) throws JsonProcessingException, ValidationExceptionVS {
        return Response.ok().entity("POST-DATA:" + new String(postData) ).build();
    }

    @POST @Path("/testSMIME")
    public Response testSMIME(MessageSMIME messageSMIME, @Context ServletContext context, @Context HttpServletRequest req,
                         @Context HttpServletResponse resp) throws JsonProcessingException, ValidationExceptionVS {
        log.info(messageSMIME.getBase64ContentDigest());
        throw new ValidationExceptionVS("Test: " + messageSMIME.getBase64ContentDigest());
    }

    @GET @Path("/")
    public Response index(@Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        return Response.ok().entity("").build();
    }

    @GET @Path("/checkLocale")
    public Response checkLocale(@Context HttpHeaders requestHeaders, @Context ServletContext context, @Context HttpServletRequest req,
                          @Context HttpServletResponse resp) {
        Locale locale = requestHeaders.getLanguage();
        return Response.ok().entity(locale.getCountry()).build();
    }


    @GET @Path("/asyncResource")
    public void asyncResource(@Suspended AsyncResponse response) {
        CompletableFuture.supplyAsync(this::getMessage, executorService).thenAccept(response::resume);
    }

    public String getMessage() {
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return "Hello from async resource";
    }

    @GET @Path("/IBAN")
    public Response IBAN(@Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws ServletException, IOException {
        String accountNumberStr = String.format("%010d", 12345L);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode("7777").branchCode( "7777")
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return Response.ok().entity(iban.toString()).build();
    }

    @GET @Path("/broadcast")
    public Response broadcast(@Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws ServletException, IOException {
        Map dataMap = new HashMap<>();
        dataMap.put("status", 200);
        dataMap.put("message", "Hello");
        dataMap.put("coreSignal", "transactionvs-new");
        SessionVSManager.getInstance().broadcast(JSON.getMapper().writeValueAsString(dataMap));
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/checkCooin")
    public Response checkCooin(@Context ServletContext context, @Context HttpServletRequest req,
                        @Context HttpServletResponse resp) throws ServletException, IOException {
        TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        auditBean.checkCurrencyRequest(timePeriod);
        return Response.ok().entity("OK").build();
    }

    @GET @Path("/logTransactions")
    public Response logTransactions(@Context ServletContext context, @Context HttpServletRequest req,
                               @Context HttpServletResponse resp) throws ServletException, IOException {
        Long init = System.currentTimeMillis();
        Random randomGenerator = new Random();
        TransactionVS.Type[] transactionTypes = TransactionVS.Type.values();
        int numTransactions = 1000;
        for (int idx = 1; idx <= numTransactions; ++idx){
            int randomInt = randomGenerator.nextInt(100);
            int transactionvsItemId = new Random().nextInt(transactionTypes.length);
            TransactionVS.Type transactionType = transactionTypes[transactionvsItemId];
            TransactionVSDto dto = new TransactionVSDto();
            dto.setId(Long.valueOf(idx));
            dto.setType(transactionType);
            dto.setFromUser("fromUser" + randomInt);
            dto.setToUser("toUser" + randomInt);
            dto.setCurrencyCode("EUR");
            dto.setAmount(new BigDecimal(randomInt));
            dto.setSubject("Subject - " + randomInt);
            dto.setDateCreated(new Date());
            LoggerVS.logTransactionVS(dto);
        }
        Long finish = System.currentTimeMillis();
        Long duration = finish - init;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        String msg = format(" --- Done numTransactions : {0} - duration in millis: {1} - duration: {2}",
                numTransactions, duration, durationStr);
        log.info(msg);
        return Response.ok().entity(msg).build();
    }

    @GET @Path("/newWeek")
    public Response newWeek() throws IOException {
        Calendar nextWeek = Calendar.getInstance();
        nextWeek.set(Calendar.WEEK_OF_YEAR, (nextWeek.get(Calendar.WEEK_OF_YEAR) + 1));
        auditBean.initWeekPeriod(nextWeek);
        /*Query query = dao.getEM().createQuery("select t from TransactionVS t where t.type  in :inList")
                .setParameter("inList", Arrays.asList(TransactionVS.Type.CURRENCY_PERIOD_INIT,
                TransactionVS.Type.CURRENCY_PERIOD_INIT_TIME_LIMITED));
        List<TransactionVS> resultList =  query.getResultList();
        for(TransactionVS transactionVS : resultList) {
            dao.getEM().remove(transactionVS);
        }*/
        return Response.ok().entity("OK").build();
    }

    @Path("/balance")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response balance() throws IOException {
        Map<String, Map<String, IncomesDto>> balancesTo = new HashMap<>();
        balancesTo.put("EUR", MapUtils.getTagMapForIncomes("HIDROGENO", new BigDecimal(880.5), new BigDecimal(700.5)));
        balancesTo.put("EUR", MapUtils.getTagMapForIncomes("NITROGENO", new BigDecimal(100), new BigDecimal(50.5)));
        balancesTo.put("DOLLAR", MapUtils.getTagMapForIncomes("WILDTAG", new BigDecimal(1454), new BigDecimal(400.5)));
        balancesTo.put("DOLLAR", MapUtils.getTagMapForIncomes("NITROGENO", new BigDecimal(100), new BigDecimal(50.5)));

        Map<String, Map<String, BigDecimal>> balancesFrom = new HashMap<>();
        balancesFrom.put("EUR", MapUtils.getTagMapForExpenses("HIDROGENO", new BigDecimal(1080.5)));
        balancesFrom.put("EUR", MapUtils.getTagMapForExpenses("OXIGENO", new BigDecimal(350)));
        balancesFrom.put("DOLLAR", MapUtils.getTagMapForExpenses("WILDTAG", new BigDecimal(6000)));
        balancesFrom.put("YEN", MapUtils.getTagMapForExpenses("WILDTAG", new BigDecimal(8000)));

        BalancesDto balancesDto = new BalancesDto();
        balancesDto.setBalancesTo(balancesTo);
        balancesDto.setBalancesFrom(balancesFrom);
        balancesDto.calculateCash();

        return Response.ok().entity(JSON.getMapper().writeValueAsBytes(balancesDto)).build();
    }

    @Path("/initWeek")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object initWeek() throws Exception {
        UserVS userVS = dao.find(UserVS.class, 2L);
        TimePeriod timePeriod = DateUtils.getWeekPeriod(DateUtils.getDayFromPreviousWeek(Calendar.getInstance()));
        auditBean.initUserVSWeekPeriod(userVS, timePeriod, "TestingController");
        return Response.ok().entity("OK").build();
    }

    @Path("/initWeekPeriod")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Object initWeekPeriod() throws Exception {
        auditBean.initWeekPeriod(Calendar.getInstance());
        return Response.ok().entity("OK").build();
    }

}
