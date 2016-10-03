package com.krystianwsul.checkme.gui;

import android.support.v4.app.Fragment;

import com.krystianwsul.checkme.MyCrashlytics;

public abstract class AbstractFragment extends Fragment {
    @Override
    public void onResume() {
        MyCrashlytics.log(getClass().getSimpleName() + ".onResume");

        super.onResume();
    }
}
