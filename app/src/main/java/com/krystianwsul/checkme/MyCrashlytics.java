package com.krystianwsul.checkme;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import junit.framework.Assert;

import io.fabric.sdk.android.Fabric;

public class MyCrashlytics {
    private static Boolean sEnabled = null;

    static void initialize(@NonNull OrganizatorApplication organizatorApplication) {
        Assert.assertTrue(sEnabled == null);

        sEnabled = organizatorApplication.getResources().getBoolean(R.bool.crashlytics_enabled);

        if (sEnabled)
            Fabric.with(organizatorApplication, new Crashlytics());
    }

    public static void log(@NonNull String message) {
        Assert.assertTrue(!TextUtils.isEmpty(message));
        Assert.assertTrue(sEnabled != null);

        if (sEnabled)
            Crashlytics.log(message);
    }

    public static void logException(@NonNull Throwable throwable) {
        Assert.assertTrue(sEnabled != null);

        if (sEnabled)
            Crashlytics.logException(throwable);
    }

    public static boolean getEnabled() {
        Assert.assertTrue(sEnabled != null);

        return sEnabled;
    }
}
