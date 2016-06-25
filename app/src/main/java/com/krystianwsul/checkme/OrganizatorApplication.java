package com.krystianwsul.checkme;

import android.app.Application;

public class OrganizatorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MyCrashlytics.initialize(this);
    }
}
