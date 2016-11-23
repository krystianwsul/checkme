package com.krystianwsul.checkme;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;

public class OrganizatorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MyCrashlytics.initialize(this);

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

        DatabaseWrapper.initialize(this);
    }
}
