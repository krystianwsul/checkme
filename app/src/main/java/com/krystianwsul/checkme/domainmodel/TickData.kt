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

    abstract fun privateTriggered()
    abstract fun sharedTriggered()

    abstract fun release()

    override fun toString() = super.toString() + " silent: $silent, source: $source"

    class Normal(
            override val silent: Boolean,
            override val source: String
    ) : TickData() {

        override var shouldClear = false
            private set

        override val waiting = false

        /*
            todo instances this made sense when there were only two database observers. Now that
            there's one for private, and sets for both shared and instances, the mechanism will
            need to be completely reworked.
         */
        override fun privateTriggered() = Unit
        override fun sharedTriggered() = Unit

        override fun release() {
            shouldClear = true
        }
    }

    class Lock(
            override val silent: Boolean,
            override val source: String,
            var waitingForPrivate: Boolean = true,
            var waitingForShared: Boolean = true
    ) : TickData() {

        companion object {

            private const val WAKELOCK_TAG = "Check.me:myWakelockTag"

            private const val DURATION = 30 * 1000
        }

        private val wakelock = (MyApplication.instance.getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)!!.apply {
            acquire(DURATION.toLong())
        }

        private val expires = DateTime.now()
                .plusMillis(DURATION)
                .toExactTimeStamp()

        override val shouldClear get() = expires < ExactTimeStamp.now || !wakelock.isHeld

        override val waiting get() = waitingForPrivate || waitingForShared

        override fun privateTriggered() {
            waitingForPrivate = false
        }

        override fun sharedTriggered() {
            waitingForShared = false
        }

        override fun release() {
            if (wakelock.isHeld)
                wakelock.release()
        }

        override fun toString() = super.toString() + ", waitingForPrivate: $waitingForPrivate, waitingForShared: $waitingForShared, expires: $expires"
    }
}
