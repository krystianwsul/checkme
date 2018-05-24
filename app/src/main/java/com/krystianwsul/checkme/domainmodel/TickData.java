package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.List;

public class TickData {
    private static final String WAKELOCK_TAG = "myWakelockTag";
    final boolean mSilent;

    @NonNull
    final String mSource;

    @NonNull
    final PowerManager.WakeLock mWakelock;

    @NonNull
    final List<Listener> listeners;

    public TickData(boolean silent, @NonNull String source, @NonNull Context context, @NonNull List<Listener> listeners) {
        Assert.assertTrue(!TextUtils.isEmpty(source));

        mSilent = silent;
        mSource = source;
        this.listeners = listeners;

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        Assert.assertTrue(powerManager != null);

        mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        mWakelock.acquire(30 * 1000);
    }

    void releaseWakelock() {
        if (mWakelock.isHeld()) mWakelock.release();
    }

    void release() {
        for (Listener listener : listeners)
            listener.onTick();

        releaseWakelock();
    }

    public interface Listener {

        void onTick();
    }
}
