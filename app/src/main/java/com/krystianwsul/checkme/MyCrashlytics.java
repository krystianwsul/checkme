package com.krystianwsul.checkme;

import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import junit.framework.Assert;

import io.fabric.sdk.android.Fabric;

public class MyCrashlytics {
    private static final boolean mEnabled = false;

    public static void initialize(OrganizatorApplication organizatorApplication) {
        Assert.assertTrue(organizatorApplication != null);

        if (mEnabled)
            Fabric.with(organizatorApplication, new Crashlytics());
    }

    public static void log(String message) {
        Assert.assertTrue(!TextUtils.isEmpty(message));

        if (mEnabled)
            Crashlytics.log(message);
    }
}
