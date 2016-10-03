package com.krystianwsul.checkme.gui;

import android.support.v7.app.AppCompatActivity;

import com.krystianwsul.checkme.MyCrashlytics;

public abstract class AbstractActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        MyCrashlytics.log(getClass().getSimpleName() + ".onResume");

        super.onResume();
    }
}
