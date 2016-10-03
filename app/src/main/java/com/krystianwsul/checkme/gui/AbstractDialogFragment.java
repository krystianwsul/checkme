package com.krystianwsul.checkme.gui;

import android.support.v4.app.DialogFragment;

import com.krystianwsul.checkme.MyCrashlytics;

public abstract class AbstractDialogFragment extends DialogFragment {
    @Override
    public void onResume() {
        MyCrashlytics.log(getClass().getSimpleName() + ".onResume");

        super.onResume();
    }
}
