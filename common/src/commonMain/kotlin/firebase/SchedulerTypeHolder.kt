package com.krystianwsul.common.firebase

interface SchedulerTypeHolder {

    companion object {

        lateinit var instance: SchedulerTypeHolder
    }

    fun get(): SchedulerType?

    fun set(schedulerType: SchedulerType)

    fun requireScheduler(requiredSchedulerType: SchedulerType? = null) {
        fun err(): Unit = throw SchedulerException() // ErrorLogger.instance.logException(SchedulerException()) // todo scheduler

        val schedulerType = get() ?: run {
            err()
            return
        }

        if (schedulerType != SchedulerType.DOMAIN) err() // todo scheduler

        requiredSchedulerType?.let {
            if (it != schedulerType) err()
        }
    }
}