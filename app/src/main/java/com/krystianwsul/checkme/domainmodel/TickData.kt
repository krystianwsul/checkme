package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.os.PowerManager
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import org.joda.time.DateTime

class TickData(
        val silent: Boolean,
        val source: String,
        val listeners: List<() -> Unit>,
        var privateRefreshed: Boolean = false,
        var sharedRefreshed: Boolean = false) {

    companion object {

        private const val WAKELOCK_TAG = "Check.me:myWakelockTag"

        private const val DURATION = 30 * 1000
    }

    val wakelock = (MyApplication.instance.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)!!.apply {
        acquire(DURATION.toLong())
    }

    val expires = ExactTimeStamp(DateTime.now().plusMillis(DURATION))

    fun releaseWakelock() {
        if (wakelock.isHeld) wakelock.release()
    }

    fun release() {
        for (listener in listeners)
            listener()

        releaseWakelock()
    }
}
