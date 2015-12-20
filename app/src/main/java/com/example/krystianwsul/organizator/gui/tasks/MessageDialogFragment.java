package com.example.krystianwsul.organizator.gui.tasks;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import junit.framework.Assert;

public class MessageDialogFragment extends DialogFragment {
    private static String MESSAGE_KEY = "message";

    public static MessageDialogFragment newInstance(String message) {
        Assert.assertTrue(!TextUtils.isEmpty(message));

        MessageDialogFragment messageDialogFragment = new MessageDialogFragment();

        Bundle args = new Bundle();
        args.putString(MESSAGE_KEY, message);
        messageDialogFragment.setArguments(args);

        return messageDialogFragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        String message = args.getString(MESSAGE_KEY);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return builder.create();
    }
}
