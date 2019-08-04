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

    private val wakelock = (MyApplication.instance.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)!!.apply {
        acquire(DURATION.toLong())
    }

    private val expires = ExactTimeStamp(DateTime.now().plusMillis(DURATION))

    val shouldClear get() = expires < ExactTimeStamp.now || !wakelock.isHeld

    fun release() {
        if (wakelock.isHeld)
            wakelock.release()
    }

    fun notifyAndRelease() {
        for (listener in listeners)
            listener()

        release()
    }
}
