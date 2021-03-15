package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.utils.ThreadInfo
import com.krystianwsul.common.utils.getThreadInfo

object DomainLocker {

    private val lock = Any()

    @Volatile
    private var lockData: LockData? = null

    fun <T> syncOnDomain(action: () -> T) = synchronized(lock) {
        throwIfLocked()
        if (lockData == null) lockData = LockData(getThreadInfo())
        lockData!!.counter++

        val ret = action()

        checkNotNull(lockData)
        lockData!!.counter--
        if (lockData!!.counter == 0) lockData = null

        ret
    }

    fun throwIfLocked() {
        lockData?.let {
            val currentThreadData = getThreadInfo()

            if (it.threadInfo.id != currentThreadData.id)
                throw DomainLockedException(it.threadInfo, currentThreadData)
        }
    }

    private class LockData(val threadInfo: ThreadInfo, var counter: Int = 0)

    /**
     * I'm betting this this whole mess is being caused by access from Firebase' thread.  If so, the next step might be
     * to
     */

    private class DomainLockedException(lockThreadInfo: ThreadInfo, currentThreadInfo: ThreadInfo) :
            Exception("locked on thread: $lockThreadInfo, current thread: $currentThreadInfo")
}