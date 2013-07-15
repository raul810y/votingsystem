package org.sistemavotacion.test.simulation;

import org.sistemavotacion.test.modelo.SimulationData;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.test.simulation.callable.EncryptionTester;
import org.sistemavotacion.test.simulation.callable.ServerInitializer;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.NifUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EncryptionSimulator extends Simulator<SimulationData> 
        implements ActionListener {
        
    private static Logger logger = LoggerFactory.getLogger(EncryptionSimulator.class);
    
    private final ExecutorService requestExecutor;
    private static CompletionService<Respuesta> requestCompletionService;
    
    List<String> representativeNifList = new ArrayList<String>();
    private String requestURL = null;
    private SimulatorListener simulationListener;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public EncryptionSimulator(SimulationData simulationData, 
            SimulatorListener simulationListener) {
        super(simulationData);
        this.simulationListener = simulationListener;
        logger.debug("NumRequestsProjected:" + simulationData.getNumRequestsProjected());
        this.requestURL = ContextoPruebas.getURLEncryptor(
                simulationData.getAccessControlURL());
        logger.debug("requestURL:" + requestURL);
        requestExecutor = Executors.newFixedThreadPool(100);
        requestCompletionService = new ExecutorCompletionService<Respuesta>(
                requestExecutor);
    }

    private void launchSimulationThreads() {
        logger.debug("launchSimulationThreads");
        simulationData.setBegin(System.currentTimeMillis());
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    launchRequests();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    readResponses();                    
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        });
    }
    
    public void launchRequests () throws Exception {
        logger.debug("- launchRequests - NumRequestsProjected: " + 
                simulationData.getNumRequestsProjected());
        if(simulationData.getNumRequestsProjected() == 0) {
            logger.debug("launchRequests - WITHOUT REQUESTS PROJECTED");
            return;
        } 
        simulationData.setBegin(System.currentTimeMillis());
        if(simulationData.isTimerBased()) startTimer(this);
        else{
            while(simulationData.getNumRequests() < 
                simulationData.getNumRequestsProjected()) {
                if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsColected()) <= simulationData.getMaxPendingResponses()) {
                    String nifFrom = NifUtils.getNif(simulationData.
                            getAndIncrementNumRequests().intValue());
                    logger.debug("launchRequests - request from: " + nifFrom);
                    requestCompletionService.submit(new EncryptionTester(
                            nifFrom, requestURL));
                 } else Thread.sleep(300);
            }
        }

    }
    
    public void readResponses() throws InterruptedException, 
            ExecutionException, Exception {
        logger.debug("--------------------- readResponses ");
        for (int v = 0; v < simulationData.getNumRequestsProjected(); v++) {
            Future<Respuesta> f = requestCompletionService.take();
            final Respuesta respuesta = f.get();
            logger.debug("Respuesta '" + v + "' statusCode: " + 
                    respuesta.getCodigoEstado());
            if(respuesta.getCodigoEstado() == Respuesta.SC_OK) {
                simulationData.getAndIncrementNumRequestsOK();
            } else {
                String msg = " - ERROR msg: " + respuesta.getMensaje();
                addErrorMsg(msg);
                logger.error(msg);
                simulationData.getAndIncrementNumRequestsERROR();
            }
        }
        countDownLatch.countDown();
    }
    
    public static void main(String[] args) throws Exception {
        SimulationData simulationData = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            simulationData = SimulationData.parse(args[0]);
        } else {
            File jsonFile = File.createTempFile("encryptionSimulationData", ".json");
            jsonFile.deleteOnExit();
            FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("simulatorFiles/encryptionSimulationData.json"), jsonFile); 
            simulationData = SimulationData.parse(FileUtils.getStringFromFile(jsonFile));
        }
        EncryptionSimulator simuHelper = new EncryptionSimulator(simulationData, null);
        simuHelper.call();
        System.exit(0);
    }

    @Override public void actionPerformed(ActionEvent ae) {
        logger.debug("timer numHoursProjected:" + simulationData.getNumHoursProjected() + 
                "- numMinutesProjected" + simulationData.getNumMinutesProjected() + 
                " - NumSecondsProjected: " + + simulationData.getNumSecondsProjected());
        if (ae.getSource().equals(timer)) {
            if((simulationData.getNumRequestsProjected() - 
                    simulationData.getNumRequestsColected()) > 0) {
                String nifFrom = NifUtils.getNif(simulationData.
                        getAndIncrementNumRequests().intValue());
                try {
                    requestCompletionService.submit(new EncryptionTester(    
                            nifFrom, requestURL));
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            } else timer.stop();
        }
    }
    
    @Override public Respuesta call() throws Exception {
        ServerInitializer accessControlInitializer = 
            new ServerInitializer(simulationData.getAccessControlURL(), 
                ActorConIP.Tipo.CONTROL_ACCESO);
        Respuesta respuesta = accessControlInitializer.call();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
             launchSimulationThreads();
        } else logger.error(respuesta.getMensaje());

        logger.debug("call - await  - process:" + ManagementFactory.
                getRuntimeMXBean().getName());
        countDownLatch.await();
        logger.debug("- call - shutdown executors");   
        

        simulationData.setFinish(System.currentTimeMillis());
        if(timer != null) timer.stop();
        if(requestExecutor != null) requestExecutor.shutdownNow();    
        logger.debug("--------------- SIMULATION RESULT----------------------"); 
        simulationData.setFinish(System.currentTimeMillis());
        simulationData.setFinish(System.currentTimeMillis());
                logger.info("Begin: " + DateUtils.getStringFromDate(
                simulationData.getBeginDate())  + " - Duration: " + 
                simulationData.getDurationStr());
        logger.debug("NumRequests: " + simulationData.getNumRequests());
        logger.debug("NumRequestsOK: " + simulationData.getNumRequestsOK());
        logger.debug("NumRequestsERROR: " + simulationData.getNumRequestsERROR());
        String errorsMsg = getFormattedErrorList();
        if(errorsMsg != null) {
            logger.info(" ************* " + errorList.size() + " ERRORS: \n" + 
                        errorsMsg);
        }
        logger.debug("--------------- FINISHED --------------------------");
        respuesta = new Respuesta(Respuesta.SC_FINALIZADO,simulationData);
        if(simulationListener != null)            
            simulationListener.processResponse(respuesta);
        return respuesta;
    }

}
