package org.votingsystem.android.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.CertRequestActivity;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.activity.IntentFilterActivity;
import org.votingsystem.android.activity.RepresentativesActivity;
import org.votingsystem.android.activity.UserCertResponseActivity;
import org.votingsystem.android.fragment.UserVSAccountsFragment;
import org.votingsystem.model.AccessControlVS;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VicketServer;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.util.GregorianCalendar;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.model.ContextVS.RESPONSEVS_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.URI_KEY;
import static org.votingsystem.model.ContextVS.URL_KEY;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VotingAppService extends Service implements Runnable {

    public static final String TAG = VotingAppService.class.getSimpleName();

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private AppContextVS appContextVS;
    private GregorianCalendar lastCheckedTime; // Time we last checked our feeds.
    private Handler handler;
    private static final int UPDATE_FREQUENCY_IN_MINUTES = 60;

    @Override public void onCreate(){
        appContextVS = (AppContextVS) getApplicationContext();
        handler = new Handler();
        Log.i(TAG + ".onCreate(...) ", "VotingAppService created");
    }

    private void processOperation (String accessControlURL, OperationVS operationVS) {
        Log.d(TAG + ".processOperation(...)", "accessControlURL: " + accessControlURL);
        ResponseVS responseVS = null;
        try {
            responseVS = HttpHelper.getData(ActorVS.getServerInfoURL(appContextVS.getVicketServerURL()),
                    ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                appContextVS.setVicketServer((VicketServer) ActorVS.parse(
                        new JSONObject(responseVS.getMessage())));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        responseVS = HttpHelper.getData(AccessControlVS.getServerInfoURL(accessControlURL),
                ContentTypeVS.JSON);
        SharedPreferences pref = getSharedPreferences(
                ContextVS.VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(ContextVS.PENDING_OPERATIONS_LAST_CHECKED_KEY, 0));
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            Intent intent = new Intent(getBaseContext(), IntentFilterActivity.class);
            responseVS.setCaption(getString(R.string.connection_error_msg));
            if(ResponseVS.SC_CONNECTION_TIMEOUT == responseVS.getStatusCode())
                responseVS.setNotificationMessage(getString(R.string.conn_timeout_msg));
            intent.putExtra(RESPONSEVS_KEY, responseVS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            try {
                AccessControlVS accessControl = AccessControlVS.parse(responseVS.getMessage());
                appContextVS.setAccessControlVS(accessControl);
                if(operationVS != null  && State.WITH_CERTIFICATE == appContextVS.getState()){
                    Log.d(TAG + ".onStartCommand(...)", "operationVS: " + operationVS.getTypeVS());
                    if(operationVS.getEventVS() != null) {
                        //We don't pass all eventvs data on uri because content can be very large
                        responseVS = HttpHelper.getData(operationVS.getEventVS().getURL(), null);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            EventVS selectedEvent = EventVS.parse(new JSONObject(responseVS.getMessage()));
                            Log.d(TAG + ".onStartCommand(...)", " _ TODO _ Fetch option selected");
                            operationVS.setEventVS(selectedEvent);
                            processOperation(operationVS);
                        }
                    } else if(operationVS.getTypeVS() == TypeVS.TRANSACTIONVS) {
                        Intent newIntent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                        newIntent.putExtra(ContextVS.FRAGMENT_KEY, UserVSAccountsFragment.class.getName());
                        newIntent.putExtra(ContextVS.URI_KEY, operationVS.getUriData());
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(newIntent);
                    }
                } else {
                    Intent intent = null;
                    SharedPreferences settings =  getSharedPreferences(
                            VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
                    switch (appContextVS.getState()) {
                        case WITHOUT_CSR:
                            String applicationID = settings.getString(APPLICATION_ID_KEY, null);
                            if (applicationID == null || applicationID.isEmpty()) {
                                Log.d(TAG + ".setActivityState() ", "applicationID: " + applicationID);
                                applicationID = UUID.randomUUID().toString();
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(APPLICATION_ID_KEY, applicationID);
                                editor.commit();
                            }
                            intent = new Intent(getBaseContext(), CertRequestActivity.class);
                            break;
                        case WITH_CSR:
                            intent = new Intent(getBaseContext(), UserCertResponseActivity.class);
                            break;
                        case WITH_CERTIFICATE:
                            intent = new Intent(getBaseContext(), RepresentativesActivity.class);
                            break;
                    }
                    if(intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void processOperation(OperationVS operationVS) {
        Log.d(TAG + ".processOperation(...)", "====_ TODO _==== operationVS: " +
                operationVS.getTypeVS() + " - state: " + appContextVS.getState());
        switch(operationVS.getTypeVS()) {
            case SEND_SMIME_VOTE:
                break;
            case MANIFEST_SIGN:
            case SMIME_CLAIM_SIGNATURE:
                break;
            default:
                Log.e(TAG + ".processOperation(...)", "unknown operationVS");;
        }
    }

    @Override public void onDestroy(){ }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG + ".onStartCommand(...) ", "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(intent != null) {
            Bundle arguments = intent.getExtras();
            final String accessControlURL = arguments.getString(URL_KEY);
            Uri uriData = (Uri) arguments.getParcelable(URI_KEY);
            OperationVS operationVS = null;
            if(uriData != null) {
                String operationStr = uriData.getQueryParameter("operation");
                if(operationStr != null) {
                    operationVS = new OperationVS(TypeVS.valueOf(operationStr), uriData);
                } else {
                    String encodedMsg = uriData.getQueryParameter("msg");
                    if(encodedMsg != null) {
                        try {
                            operationVS = OperationVS.parse(StringUtils.decodeString(encodedMsg));
                        } catch(Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            final OperationVS operationFinal = operationVS;
            Runnable runnable = new Runnable() {
                @Override public void run() {
                    processOperation(accessControlURL, operationFinal);
                }
            };
            /*Services run in the main thread of their hosting process. This means that, if
            * it's going to do any CPU intensive (such as networking) operations, it should
            * spawn its own thread in which to do that work.*/
            Thread thr = new Thread(null, runnable, "voting_app_service_thread");
            thr.start();
            //We want this service to continue running until it is explicitly stopped, so return sticky.
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent){
        return mBinder;
    }

    private final IBinder mBinder = new Binder()  {
        @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    };

    private void checkForPendingOperations() {
        Log.d(TAG + ".checkForPendingOperations()", "");
        GregorianCalendar nextCheckTime = new GregorianCalendar();
        nextCheckTime = (GregorianCalendar) lastCheckedTime.clone();
        nextCheckTime.add(GregorianCalendar.MINUTE, UPDATE_FREQUENCY_IN_MINUTES);
        Log.d(TAG + ".checkForPendingOperations() ", "last checked time:" +
                lastCheckedTime.toString() + " - Next checked time: " + nextCheckTime.toString());

        if(lastCheckedTime.before(nextCheckTime)) runPendingOperations();
        else handler.postDelayed(this, 1000 * 60 * UPDATE_FREQUENCY_IN_MINUTES);
    }

    private void runPendingOperations() {
        Log.d(TAG + ".checkForPendingOperations(...) ", "");

    }

    @Override public void run() {
        checkForPendingOperations();
    }

}