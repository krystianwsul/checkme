package com.krystianwsul.checkme;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class MyCrashlytics {
    private static final boolean mEnabled = true;

    public static void initialize(OrganizatorApplication organizatorApplication) {
        if (mEnabled)
            Fabric.with(organizatorApplication, new Crashlytics());
    }

    public static void log(String message) {
        if (mEnabled)
            Crashlytics.log(message);
    }
}
