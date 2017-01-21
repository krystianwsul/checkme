package com.krystianwsul.checkme;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.squareup.leakcanary.LeakCanary;

public class OrganizatorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this))
            return;
        LeakCanary.install(this);

        MyCrashlytics.initialize(this);

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        DatabaseWrapper.initialize(this);
    }
}
