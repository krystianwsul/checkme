package com.krystianwsul.checkme;

import android.app.Application;
import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

public class OrganizatorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MyCrashlytics.initialize(this);

        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

        getSharedPreferences("asdf", Context.MODE_PRIVATE)
                .edit()
                .putString("asdf", null)
                .apply();
    }
}
