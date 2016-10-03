package com.krystianwsul.checkme.gui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.krystianwsul.checkme.R;

import junit.framework.Assert;

public class DiscardDialogFragment extends AbstractDialogFragment {
    private DiscardDialogListener mDiscardDialogListener;

    public static DiscardDialogFragment newInstance() {
        return new DiscardDialogFragment();
    }

    public DiscardDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .content(R.string.discard_changes)
                .negativeText(android.R.string.cancel)
                .positiveText(R.string.discard)
                .onPositive((dialog, which) -> {
                    Assert.assertTrue(mDiscardDialogListener != null);
                    mDiscardDialogListener.onYes();
                })
                .show();
    }

    public void setDiscardDialogListener(DiscardDialogListener discardDialogListener) {
        Assert.assertTrue(discardDialogListener != null);
        mDiscardDialogListener = discardDialogListener;
    }

    public interface DiscardDialogListener {
        void onYes();
    }
}
