package com.krystianwsul.common.domain

import com.krystianwsul.common.time.*

sealed class InstanceData<T, U, V : InstanceRecord<U>> {

    abstract val scheduleDate: Date
    abstract val scheduleTime: Time

    abstract val instanceDate: Date
    abstract val instanceTime: Time

    abstract val done: Long?

    abstract val hidden: Boolean

    abstract class Real<T, U, V : InstanceRecord<U>>(val instanceRecord: V) : InstanceData<T, U, V>() {

        protected abstract fun getCustomTime(customTimeId: U): CustomTime

        protected abstract fun getSignature(): String

        override val scheduleDate get() = instanceRecord.let { Date(it.scheduleYear, it.scheduleMonth, it.scheduleDay) }

        override val scheduleTime
            get() = instanceRecord.run {
                scheduleCustomTimeId?.let { getCustomTime(it) }
                        ?: NormalTime(scheduleHour!!, scheduleMinute!!)
            }

        override val instanceDate get() = instanceRecord.instanceDate ?: scheduleDate

        override val instanceTime
            get() = instanceRecord.instanceJsonTime
                    ?.let {
                        when (it) {
                            is JsonTime.Custom -> getCustomTime(it.id)
                            is JsonTime.Normal -> NormalTime(it.hourMinute)
                        }
                    }
                    ?: scheduleTime

        override val done get() = instanceRecord.done

        override val hidden get() = instanceRecord.hidden
    }

    class Virtual<T, U, V : InstanceRecord<U>>(val taskId: T, val scheduleDateTime: DateTime) : InstanceData<T, U, V>() {

        override val scheduleDate by lazy { scheduleDateTime.date }

        override val scheduleTime = scheduleDateTime.time

        override val instanceDate by lazy { scheduleDate }

        override val instanceTime = scheduleTime

        override val done: Long? = null

        override val hidden = false
    }
}

