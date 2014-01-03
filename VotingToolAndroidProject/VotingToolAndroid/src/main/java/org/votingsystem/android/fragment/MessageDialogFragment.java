package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class MessageDialogFragment extends DialogFragment {

    public static final String TAG = "MessageDialogFragment";

    public static MessageDialogFragment newInstance(Integer statusCode, String caption,
                    String message){
        MessageDialogFragment frag = new MessageDialogFragment();
        Bundle args = new Bundle();
        if(statusCode != null) args.putInt(ContextVS.RESPONSE_STATUS_KEY, statusCode);
        args.putString(ContextVS.CAPTION_KEY, caption);
        args.putString(ContextVS.MESSAGE_KEY, message);
        frag.setArguments(args);
        return frag;
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        int statusCode = getArguments().getInt(ContextVS.RESPONSE_STATUS_KEY, -1);
        String caption = getArguments().getString(ContextVS.CAPTION_KEY);
        String message = getArguments().getString(ContextVS.MESSAGE_KEY);
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
        if(caption != null) builder.setTitle(caption);
        if(message != null) builder.setMessage(message);
        AlertDialog dialog = builder.create();
        if(statusCode > 0) {
            if(ResponseVS.SC_OK == statusCode) dialog.setIcon(R.drawable.accept_16);
            else dialog.setIcon(R.drawable.cancel_16);
        }
        return dialog;
    }

}
