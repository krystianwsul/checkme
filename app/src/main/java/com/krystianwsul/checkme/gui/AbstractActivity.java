package com.krystianwsul.checkme.gui;

import android.support.v7.app.AppCompatActivity;

import com.krystianwsul.checkme.MyCrashlytics;

import io.reactivex.disposables.CompositeDisposable;

public abstract class AbstractActivity extends AppCompatActivity {

    protected CompositeDisposable createDisposable = new CompositeDisposable();

    @Override
    protected void onResume() {
        MyCrashlytics.log(getClass().getSimpleName() + ".onResume");

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        createDisposable.dispose();

        super.onDestroy();
    }
}
