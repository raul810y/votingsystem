package org.votingsystem.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.android.fragment.EventVSFragment;
import org.votingsystem.android.fragment.VotingEventFragment;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import static org.votingsystem.model.ContextVS.CURSOR_POSITION_KEY;
import static org.votingsystem.model.ContextVS.EVENT_STATE_KEY;
import static org.votingsystem.model.ContextVS.TYPEVS_KEY;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class EventVSPagerActivity extends FragmentActivity {

    public static final String TAG = EventVSPagerActivity.class.getSimpleName();

    private Cursor cursor = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG + ".broadcastReceiver", "intentExtras:" + intent.getExtras());
        }
    };


    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_activity);
        Integer cursorPosition = getIntent().getIntExtra(CURSOR_POSITION_KEY, -1);
        String eventStateStr = getIntent().getStringExtra(EVENT_STATE_KEY);
        String eventTypeStr = getIntent().getStringExtra(TYPEVS_KEY);

        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                EventVSContentProvider.STATE_COL + "= ? ";
        cursor = getContentResolver().query(EventVSContentProvider.CONTENT_URI,
                null, selection, new String[]{eventTypeStr, eventStateStr}, null);
        cursor.moveToPosition(cursorPosition);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        EventsPagerAdapter eventsPagerAdapter = new EventsPagerAdapter(getSupportFragmentManager(),
                eventStateStr, eventTypeStr);
        mViewPager.setAdapter(eventsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                cursor.moveToPosition(position);
                setActionBarTitle();
            }
        });
        mViewPager.setCurrentItem(cursorPosition);
        setActionBarTitle();
    }

    private void setActionBarTitle() {
        String eventJSON = cursor.getString(cursor.getColumnIndex(
                EventVSContentProvider.JSON_DATA_COL));
        EventVS event = null;
        try {
            event = EventVS.parse(new JSONObject(eventJSON));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        String subtTitle = null;
        switch(event.getTypeVS()) {
            case MANIFEST_EVENT:
                getActionBar().setLogo(R.drawable.manifest_32);
                switch(event.getState()) {
                    case ACTIVE:
                        getActionBar().setTitle(getString(R.string.manifest_open_lbl,
                                DateUtils.getElapsedTimeStr(event.getDateFinish())));
                        break;
                    case PENDING:
                        getActionBar().setTitle(getString(R.string.manifest_pendind_lbl));
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish());
                        break;
                    case CANCELLED:
                        getActionBar().setTitle(getString(R.string.manifest_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case TERMINATED:
                        getActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish());
                        break;
                    default:
                        getActionBar().setTitle(getString(R.string.manifest_closed_lbl));
                }
                break;
            case CLAIM_EVENT:
                getActionBar().setLogo(R.drawable.fa_exclamation_triangle_32);
                switch(event.getState()) {
                    case ACTIVE:
                        getActionBar().setTitle(getString(R.string.claim_open_lbl,
                                DateUtils.getElapsedTimeStr(event.getDateFinish())));
                        break;
                    case PENDING:
                        getActionBar().setTitle(getString(R.string.claim_pending_lbl));
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish());
                        break;
                    case CANCELLED:
                        getActionBar().setTitle(getString(R.string.claim_closed_lbl) + " - (" +
                                getString(R.string.event_canceled) + ")");
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish()) +
                                " (" +  getString(R.string.event_canceled)  + ")";
                        break;
                    case TERMINATED:
                        setTitle(getString(R.string.claim_closed_lbl));
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish());
                    default:
                        getActionBar().setTitle(getString(R.string.claim_closed_lbl));
                }
                break;
            case VOTING_EVENT:
                getActionBar().setLogo(R.drawable.poll_32);
                switch(event.getState()) {
                    case ACTIVE:
                        getActionBar().setTitle(getString(R.string.voting_open_lbl,
                                DateUtils.getElapsedTimeStr(event.getDateFinish())));
                        break;
                    case PENDING:
                        getActionBar().setTitle(getString(R.string.voting_pending_lbl));
                        subtTitle = getString(R.string.init_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateBegin()) + " - " +
                                "" + getString(R.string.finish_lbl) + ": " +
                                DateUtils.getDayWeekDateStr(event.getDateFinish());
                        break;
                    default:
                        getActionBar().setTitle(getString(R.string.voting_closed_lbl));
                }
                break;
        }
        if(subtTitle != null) getActionBar().setSubtitle(subtTitle);
    }

    @Override public AppContextVS getApplicationContext() {
        return (AppContextVS) super.getApplicationContext();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class EventsPagerAdapter extends FragmentStatePagerAdapter {

        private Cursor cursor;
        private TypeVS eventType;

        public EventsPagerAdapter(FragmentManager fm, String eventStateStr, String eventTypeStr) {
            super(fm);
            eventType = TypeVS.valueOf(eventTypeStr);
            String selection = EventVSContentProvider.TYPE_COL + "=? AND " +
                    EventVSContentProvider.STATE_COL + "= ? ";
            cursor = getContentResolver().query(EventVSContentProvider.CONTENT_URI,
                    null, selection, new String[]{eventTypeStr, eventStateStr}, null);
        }

        @Override public Fragment getItem(int i) {
            Log.d(TAG + ".EventsPagerAdapter.getItem(...) ", "item: " + i);
            cursor.moveToPosition(i);
            String eventJSONStr = cursor.getString(cursor.getColumnIndex(
                    EventVSContentProvider.JSON_DATA_COL));
            if(TypeVS.VOTING_EVENT == eventType) return VotingEventFragment.newInstance(eventJSONStr);
            else return EventVSFragment.newInstance(eventJSONStr);
            //return EventVSDBViewerFragment.newInstance(eventJSONStr);
        }

        @Override public int getCount() {
            return cursor.getCount();
        }

    }

}