package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.os.PowerManager

class TickData(val silent: Boolean, val source: String, context: Context, val listeners: List<() -> Unit>) {

    companion object {

        private const val WAKELOCK_TAG = "myWakelockTag"
    }

    val wakelock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)!!.apply {
        acquire((30 * 1000).toLong())
    }

    fun releaseWakelock() {
        if (wakelock.isHeld) wakelock.release()
    }

    fun release() {
        for (listener in listeners)
            listener()

        releaseWakelock()
    }
}
