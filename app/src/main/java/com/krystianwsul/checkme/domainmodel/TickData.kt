package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.os.PowerManager
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.utils.time.toExactTimeStamp
import com.krystianwsul.common.time.ExactTimeStamp
import org.joda.time.DateTime

sealed class TickData {

    abstract val notifierParams: Notifier.Params

    abstract val shouldClear: Boolean

    abstract val waiting: Boolean

    abstract val domainChanged: Boolean

    abstract fun release()

    override fun toString() = super.toString() + ", notifierParams: $notifierParams"

    class Normal(override val notifierParams: Notifier.Params) : TickData() {

        override var shouldClear = false
            private set

        override val waiting = false

        override val domainChanged = false

        override fun release() {
            shouldClear = true
        }
    }

    class Lock(
        override val notifierParams: Notifier.Params,
        override val domainChanged: Boolean,
        val expires: ExactTimeStamp.Local = DateTime.now()
            .plusMillis(DURATION)
            .toExactTimeStamp(),
    ) : TickData() {

        init {
            Preferences.fcmLog.logLineHour("acquiring lock until $expires")
        }

        companion object {

            private const val WAKELOCK_TAG = "Check.me:myWakelockTag"

            private const val DURATION = 30 * 1000
        }

        private val wakelock = (MyApplication.instance.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        )!!.apply {
            acquire(expires.long - ExactTimeStamp.Local.now.long)
        }

        override val shouldClear get() = expires < ExactTimeStamp.Local.now || !wakelock.isHeld

        override val waiting = true

        override fun release() {
            Preferences.fcmLog.logLineHour("releasing wakeLock, isHeld? " + wakelock.isHeld)

            if (wakelock.isHeld)
                wakelock.release()
        }

        override fun toString() = super.toString() + ", expires: $expires"
    }
}
