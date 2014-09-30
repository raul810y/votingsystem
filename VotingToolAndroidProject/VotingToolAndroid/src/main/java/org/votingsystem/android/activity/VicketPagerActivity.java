package org.votingsystem.android.activity;

import android.app.SearchManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.VicketContentProvider;
import org.votingsystem.android.fragment.TransactionVSGridFragment;
import org.votingsystem.android.fragment.UserVSAccountsFragment;
import org.votingsystem.android.fragment.VicketFragment;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.android.util.UIUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.DateUtils;

import java.util.Calendar;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketPagerActivity extends ActivityBase {

    public static final String TAG = VicketPagerActivity.class.getSimpleName();

    private AppContextVS contextVS;
    private Cursor cursor = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getApplicationContext();
        setContentView(R.layout.pager_activity);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int cursorPosition = getIntent().getIntExtra(ContextVS.CURSOR_POSITION_KEY, 0);
        Log.d(TAG + ".onCreate(...) ", "cursorPosition: " + cursorPosition +
                " - savedInstanceState: " + savedInstanceState);
        VicketPagerAdapter pagerAdapter = new VicketPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        cursor = getContentResolver().query(VicketContentProvider.CONTENT_URI,null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        mViewPager.setCurrentItem(cursorPosition);
        getSupportActionBar().setLogo(UIUtils.getLogoIcon(this, R.drawable.fa_money_32));
        getSupportActionBar().setTitle(getString(R.string.vicket_lbl));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState:" + outState);
    }

    @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG + ".onRestoreInstanceState(...)", "onRestoreInstanceState:" + savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
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

    class VicketPagerAdapter extends FragmentStatePagerAdapter {

        final String TAG = VicketPagerAdapter.class.getSimpleName();

        private NavigatorDrawerOptionsAdapter.ChildPosition selectedChild = null;
        private NavigatorDrawerOptionsAdapter.GroupPosition selectedGroup =
                NavigatorDrawerOptionsAdapter.GroupPosition.VICKETS;

        private String searchQuery = null;

        public VicketPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override public Fragment getItem(int position) {
            NavigatorDrawerOptionsAdapter.ChildPosition childPosition = selectedGroup.getChildList().get(position);
            Fragment selectedFragment = null;
            switch(childPosition) {
                case VICKET_USER_INFO:
                    selectedFragment = new UserVSAccountsFragment();
                    break;
                case VICKET_LIST:
                    selectedFragment = new TransactionVSGridFragment();
                    break;
            }
            Bundle args = new Bundle();
            args.putString(SearchManager.QUERY, searchQuery);
            selectedFragment.setArguments(args);
            Log.d(TAG + ".getItem(...) ", "position:" + position + " - args: " + args +
                    " - selectedFragment.getClass(): " + ((Object)selectedFragment).getClass());
            return selectedFragment;
        }

        public String getSelectedChildDescription(AppContextVS context) {
            switch(selectedChild) {
                case VICKET_USER_INFO:
                    DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance());
                    String periodLbl = context.getString(R.string.week_lapse_lbl, DateUtils.getDate_Es(
                            timePeriod.getDateFrom()), DateUtils.getDate_Es(timePeriod.getDateTo()));
                    return periodLbl;
                case VICKET_LIST:
                    return context.getString(R.string.vickets_list_lbl);
                default:
                    return context.getString(R.string.unknown_event_state_lbl);
            }
        }

        public String getSelectedGroupDescription(AppContextVS context) {
            return selectedGroup.getDescription(context);
        }

        public void setSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
        }

        public void selectItem(Integer groupPosition, Integer childPosition) {
            selectedChild = selectedGroup.getChildList().get(childPosition);
        }

        public void updateChildPosition(int childPosition) {
            selectedChild = selectedGroup.getChildList().get(childPosition);
        }

        public int getSelectedChildPosition() {
            return selectedGroup.getChildList().indexOf(selectedChild);
        }

        public int getSelectedGroupPosition() {
            return selectedGroup.getPosition();
        }

        public Drawable getLogo(AppContextVS context) {
            return context.getResources().getDrawable(selectedGroup.getLogo());
        }

        @Override public int getCount() {
            return selectedGroup.getChildList().size();
        }


    }

}