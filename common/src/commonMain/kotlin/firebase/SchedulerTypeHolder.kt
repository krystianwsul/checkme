package com.krystianwsul.common.firebase

interface SchedulerTypeHolder {

    companion object {

        lateinit var instance: SchedulerTypeHolder

        fun requireScheduler(requiredSchedulerType: SchedulerType? = null) {
            val schedulerType = instance.get() ?: throw SchedulerException() // todo scheduler

            requiredSchedulerType?.let {
                if (it != schedulerType) throw SchedulerException() // todo scheduler
            }
        }
    }

    fun get(): SchedulerType?

    fun set(schedulerType: SchedulerType)
}