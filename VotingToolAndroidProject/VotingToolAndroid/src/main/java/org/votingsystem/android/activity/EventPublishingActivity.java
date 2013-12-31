package org.votingsystem.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventPublishingFragment;
import org.votingsystem.model.OperationVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventPublishingActivity extends ActionBarActivity {

    public static final String TAG = "EventPublishingActivity";

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.generic_fragment_container_activity);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        EventPublishingFragment publishFragment = new EventPublishingFragment();
        publishFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                publishFragment, EventPublishingFragment.TAG).commit();
    }

    public void processOperation(OperationVS operationVS) {
        EventPublishingFragment fragment = (EventPublishingFragment)getSupportFragmentManager().
                findFragmentByTag(EventPublishingFragment.TAG);
        if (fragment != null) fragment.processOperation(operationVS);

    }

    /*@Override public void onBackPressed() {
        EventPublishingFragment fragment = (EventPublishingFragment)getSupportFragmentManager().
                findFragmentByTag(EventPublishingFragment.TAG);
        if (fragment != null) fragment.onBackPressed();
        else  super.onBackPressed();
    }*/

    //@Override public boolean onKeyDown(int keyCode, KeyEvent event) {}

}