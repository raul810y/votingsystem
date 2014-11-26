package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.EventVSStatsPagerActivity;
import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.util.WalletUtils;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ResponseVS;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.FRAGMENT_KEY;

public class WalletFragment extends Fragment {

    public static final String TAG = WalletFragment.class.getSimpleName();
    public static final String AUTHENTICATED_KEY = "AUTHENTICATED_KEY";

    private View rootView;
    private GridView gridView;
    private VicketListAdapter adapter = null;
    private List<Vicket> vicketList;
    private String broadCastId = WalletFragment.class.getSimpleName();
    private String actualPIN;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        if(pin != null) {
            switch(responseVS.getTypeVS()) {
                case VICKET:
                    try {
                        actualPIN = pin;
                        vicketList = WalletUtils.getVicketList(pin, getActivity());
                        adapter.setItemList(vicketList);
                        adapter.notifyDataSetChanged();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MessageDialogFragment.showDialog(ResponseVS.SC_ERROR,
                                getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                    }
                    break;
                case PIN_CHANGE:
                    try {
                        WalletUtils.changeWalletPin(pin, actualPIN, getActivity());
                        MessageDialogFragment.showDialog(getString(R.string.change_wallet_pin),
                                getString(R.string.operation_ok_msg), getFragmentManager());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MessageDialogFragment.showDialog(ResponseVS.getExceptionResponse(
                                ex, getActivity()), getFragmentManager());
                    }
                    break;
            }
        } else {
            switch(responseVS.getTypeVS()) {
                case VICKET_USER_INFO:
                    break;
            }
        }
        }
    };

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<UserVS> ALPHA_COMPARATOR = new Comparator<UserVS>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(UserVS object1, UserVS object2) {
            return sCollator.compare(object1.getName(), object2.getName());
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        ((FragmentContainerActivity)getActivity()).setTitle(getString(R.string.wallet_lbl), null, null);
        rootView = inflater.inflate(R.layout.grid_container, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(FRAGMENT_KEY, VicketFragment.class.getName());
                intent.putExtra(ContextVS.VICKET_KEY, vicketList.get(position));
                startActivity(intent);
            }
        });
        vicketList = WalletUtils.getVicketList();
        if(vicketList == null) {
            PinDialogFragment.showWalletPinScreen(getFragmentManager(), broadCastId,
                    getString(R.string.enter_wallet_pin_msg), false, TypeVS.VICKET);
            vicketList = new ArrayList<Vicket>();
        }
        adapter = new VicketListAdapter(vicketList, getActivity());
        gridView.setAdapter(adapter);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.wallet, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_wallet_pin:
                PinDialogFragment.showWalletPinScreenWithoutHashValidation(getFragmentManager(),
                        broadCastId, getString(R.string.enter_new_pin_wallet_msg), TypeVS.PIN_CHANGE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void isProgressDialogVisible(boolean isVisible) {
        if(isVisible) ProgressDialogFragment.showDialog(
                getString(R.string.unlocking_wallet_msg), getString(R.string.wait_msg), getFragmentManager());
        else ProgressDialogFragment.hide(getFragmentManager());
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        Intent intent = activity.getIntent();
        if(intent != null) {
            String query = null;
            if (Intent.ACTION_SEARCH.equals(intent)) {
                query = intent.getStringExtra(SearchManager.QUERY);
            }
            LOGD(TAG + ".onAttach", "activity: " + activity.getClass().getName() +
                    " - query: " + query + " - activity: ");
        }
    }

    public class VicketListAdapter  extends ArrayAdapter<Vicket> {

        private List<Vicket> itemList;
        private Context context;

        public VicketListAdapter(List<Vicket> itemList, Context ctx) {
            super(ctx, R.layout.vicket_card, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public Vicket getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        @Override public View getView(int position, View view, ViewGroup parent) {
            Vicket vicket = itemList.get(position);
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.vicket_card, null);
            }
            //Date weekLapse = DateUtils.getDateFromPath(weekLapseStr);
            //Calendar weekLapseCalendar = Calendar.getInstance();
            //weekLapseCalendar.setTime(weekLapse);
            LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
            linearLayout.setBackgroundColor(Color.WHITE);
            TextView date_data = (TextView)view.findViewById(R.id.date_data);
            String dateData = getString(R.string.vicket_data_info_lbl,
                    DateUtils.getDayWeekDateStr(vicket.getDateFrom()),
                    DateUtils.getDayWeekDateStr(vicket.getDateTo()));
            date_data.setText(DateUtils.getDayWeekDateStr(vicket.getDateFrom()));

            TextView vicket_state = (TextView) view.findViewById(R.id.vicket_state);
            vicket_state.setText(vicket.getState().toString());
            TextView week_lapse = (TextView) view.findViewById(R.id.week_lapse);
            //week_lapse.setText(weekLapseStr);

            TextView amount = (TextView) view.findViewById(R.id.amount);
            amount.setText(vicket.getAmount().toPlainString());
            TextView currency = (TextView) view.findViewById(R.id.currencyCode);
            currency.setText(vicket.getCurrencyCode().toString());
            return view;
        }

        public List<Vicket> getItemList() {
            return itemList;
        }

        public void setItemList(List<Vicket> itemList) {
            this.itemList = itemList;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

}