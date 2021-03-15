package com.krystianwsul.common.firebase

import com.krystianwsul.common.ErrorLogger

interface SchedulerTypeHolder {

    companion object {

        lateinit var instance: SchedulerTypeHolder
    }

    fun get(): SchedulerType?

    fun set(schedulerType: SchedulerType)

    fun requireScheduler(requiredSchedulerType: SchedulerType? = null) {
        fun err() = ErrorLogger.instance.logException(SchedulerException())

        val schedulerType = get() ?: run {
            err()
            return
        }

        requiredSchedulerType?.let {
            if (it != schedulerType) err()
        }
    }
}