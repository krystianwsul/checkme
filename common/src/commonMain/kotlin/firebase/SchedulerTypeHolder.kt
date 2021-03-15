package com.krystianwsul.common.firebase

interface SchedulerTypeHolder {

    companion object {

        lateinit var instance: SchedulerTypeHolder
    }

    fun get(): SchedulerType?

    fun set(schedulerType: SchedulerType)

    fun requireScheduler(requiredSchedulerType: SchedulerType? = null) {
        val schedulerType = get() ?: throw SchedulerException() // todo scheduler

        requiredSchedulerType?.let {
            if (it != schedulerType) throw SchedulerException() // todo scheduler
        }
    }
}