package com.krystianwsul.common.firebase

interface SchedulerTypeHolder {

    companion object {

        lateinit var instance: SchedulerTypeHolder
    }

    fun get(): SchedulerType?

    fun set(schedulerType: SchedulerType)
}