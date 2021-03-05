package com.krystianwsul.checkme.domainmodel

object DomainLocker {

    private val lock = Any()

    private fun getCurrentThreadData() = Thread.currentThread().let { ThreadData(it.id, it.name) }

    @Volatile
    private var lockData: LockData? = null

    fun <T> syncOnDomain(action: () -> T) = synchronized(lock) {
        throwIfLocked()
        if (lockData == null) lockData = LockData(getCurrentThreadData())
        lockData!!.counter++

        val ret = action()

        checkNotNull(lockData)
        lockData!!.counter--
        if (lockData!!.counter == 0) lockData = null

        ret
    }

    fun throwIfLocked() {
        lockData?.let {
            val currentThreadData = getCurrentThreadData()

            if (it.threadData.threadId != currentThreadData.threadId)
                throw DomainLockedException(it.threadData, currentThreadData)
        }
    }

    private class LockData(val threadData: ThreadData, var counter: Int = 0)

    private data class ThreadData(val threadId: Long, val threadName: String)

    /**
     * I'm betting this this whole mess is being caused by access from Firebase' thread.  If so, the next step might be
     * to
     */

    private class DomainLockedException(lockThreadData: ThreadData, currentThreadData: ThreadData) :
            Exception("locked on thread: $lockThreadData, current thread: $currentThreadData")
}