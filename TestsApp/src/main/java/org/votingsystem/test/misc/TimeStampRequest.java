package org.votingsystem.test.misc;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TimeStampRequest {

    private static Logger log =  Logger.getLogger(TimeStampRequest.class.getName());

    private static SimulationData simulationData;
    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        simulationData = new SimulationData();
        simulationData.setServerURL("https://192.168.1.5/TimeStampServer");
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(100);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);

        ResponseVS responseVS = HttpHelper.getInstance().getData(Actor.getServerInfoURL(
                simulationData.getServerURL()), ContentType.JSON);
        if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        Actor actor = ((ActorDto)responseVS.getMessage(ActorDto.class)).getActor();
        ContextVS.getInstance().setDefaultServer(actor);
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.info("NumRequestsProjected = 0");
            return;
        }
        log.info("initSimulation - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        completionService = new ExecutorCompletionService<ResponseVS>(executorService);
        executorService.execute(() -> {
            try {
                sendRequests();
            } catch (Exception e) { e.printStackTrace();  }
        });
        executorService.execute(() -> {
            try {
                waitForResponses();
            } catch (Exception e) { e.printStackTrace();  }
        });
    }

    public static void sendRequests() throws Exception {
        log.info("sendRequests - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while(simulationData.getNumRequests() < simulationData.getNumRequestsProjected()) {
            if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsCollected()) <= simulationData.getMaxPendingResponses()) {
                completionService.submit(() -> {
                    String nifFrom = NifUtils.getNif(simulationData.getAndIncrementNumRequests().intValue());
                    Map map = new HashMap();
                    map.put("operation", "TIMESTAMP_TEST");
                    map.put("nif", nifFrom);
                    map.put("UUID", UUID.randomUUID().toString());
                    byte[] request = JSON.getMapper().writeValueAsBytes(map);
                    SignatureService signatureService = SignatureService.load(nifFrom);
                    CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(request);
                    return ResponseVS.OK().setCMS(cmsMessage);
                });
            } else Thread.sleep(300);
        }
    }

    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = completionService.take();
                ResponseVS responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else simulationData.finishAndExit(ResponseVS.SC_ERROR, "ERROR: " + responseVS.getMessage());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                simulationData.finishAndExit(ResponseVS.SC_EXCEPTION, "EXCEPTION: " + ex.getMessage());
            }
        }
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }

}
    





