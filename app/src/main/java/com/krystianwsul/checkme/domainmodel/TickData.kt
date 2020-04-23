package com.krystianwsul.checkme.domainmodel

import android.content.Context
import android.os.PowerManager
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.utils.time.toExactTimeStamp
import com.krystianwsul.common.time.ExactTimeStamp
import org.joda.time.DateTime

sealed class TickData {

    abstract val silent: Boolean
    abstract val source: String

    abstract val shouldClear: Boolean

    abstract val waiting: Boolean

    abstract fun release()

    override fun toString() = super.toString() + " silent: $silent, source: $source"

    class Normal(
            override val silent: Boolean,
            override val source: String
    ) : TickData() {

        override var shouldClear = false
            private set

        override val waiting = false

        override fun release() {
            shouldClear = true
        }
    }

    class Lock(
            override val silent: Boolean,
            override val source: String,
            val expires: ExactTimeStamp = DateTime.now()
                    .plusMillis(DURATION)
                    .toExactTimeStamp()
    ) : TickData() {

        companion object {

            private const val WAKELOCK_TAG = "Check.me:myWakelockTag"

            private const val DURATION = 30 * 1000
        }

        private val wakelock = (MyApplication.instance.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)!!.apply {
            acquire(expires.long - ExactTimeStamp.now.long)
        }

        override val shouldClear get() = expires < ExactTimeStamp.now || !wakelock.isHeld

        override val waiting = true

        override fun release() {
            if (wakelock.isHeld)
                wakelock.release()
        }

        override fun toString() = super.toString() + ", expires: $expires"
    }
}
