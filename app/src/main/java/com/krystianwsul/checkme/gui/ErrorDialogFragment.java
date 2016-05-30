package com.krystianwsul.checkme.gui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.krystianwsul.checkme.R;

import junit.framework.Assert;

import java.util.ArrayList;

public class ErrorDialogFragment extends DialogFragment {
    private final static String ERRORS_KEY = "errors";

    public static ErrorDialogFragment newInstance(ArrayList<String> errors) {
        Assert.assertTrue(errors != null);
        Assert.assertTrue(!errors.isEmpty());

        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(ERRORS_KEY, errors);
        errorDialogFragment.setArguments(args);

        return errorDialogFragment;
    }

    public ErrorDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(ERRORS_KEY));

        ArrayList<String> errors = args.getStringArrayList(ERRORS_KEY);
        Assert.assertTrue(errors != null);

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.error)
                .content(TextUtils.join("\n", errors))
                .positiveText(android.R.string.ok)
                .show();
    }
}
