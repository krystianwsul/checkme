package com.krystianwsul.checkme;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import junit.framework.Assert;

import io.fabric.sdk.android.Fabric;

public class MyCrashlytics {
    private static Boolean sEnabled = null;

    static void initialize(@NonNull MyApplication myApplication) {
        Assert.assertTrue(sEnabled == null);

        sEnabled = myApplication.getResources().getBoolean(R.bool.crashlytics_enabled);

        if (sEnabled)
            Fabric.with(myApplication, new Crashlytics());
    }

    public static void initialize() {
        Assert.assertTrue(sEnabled == null);

        sEnabled = false;
    }

    public static void log(@NonNull String message) {
        Assert.assertTrue(!TextUtils.isEmpty(message));
        Assert.assertTrue(sEnabled != null);

        Log.e("asdf", "MyCrashLytics.log: " + message);
        if (sEnabled)
            Crashlytics.log(message);
    }

    public static void logException(@NonNull Throwable throwable) {
        Assert.assertTrue(sEnabled != null);

        Log.e("asdf", "MyCrashLytics.logException", throwable);
        if (sEnabled)
            Crashlytics.logException(throwable);
    }

    public static boolean getEnabled() {
        Assert.assertTrue(sEnabled != null);

        return sEnabled;
    }
}
