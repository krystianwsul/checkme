package com.krystianwsul.checkme.utils

import com.krystianwsul.checkme.domainmodel.CustomTime
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.InstanceRecord
import com.krystianwsul.checkme.utils.time.*

sealed class InstanceData<T, U, V : InstanceRecord<U>> {

    abstract val scheduleDate: Date
    abstract fun getScheduleTime(domainFactory: DomainFactory): Time

    abstract val instanceDate: Date
    abstract fun getInstanceTime(domainFactory: DomainFactory): Time

    abstract val done: Long?

    abstract class RealInstanceData<T, U, V : InstanceRecord<U>>(val instanceRecord: V) : InstanceData<T, U, V>() {

        protected abstract fun getCustomTime(customTimeId: U): CustomTime

        protected abstract fun getSignature(): String

        override val scheduleDate get() = instanceRecord.let { Date(it.scheduleYear, it.scheduleMonth, it.scheduleDay) }

        override fun getScheduleTime(domainFactory: DomainFactory): Time {
            val customTimeId = instanceRecord.scheduleCustomTimeId
            val hour = instanceRecord.scheduleHour
            val minute = instanceRecord.scheduleMinute

            check(hour == null == (minute == null))
            check(customTimeId == null != (hour == null))

            return customTimeId?.let { getCustomTime(it) } ?: NormalTime(hour!!, minute!!)
        }

        override val instanceDate get() = instanceRecord.instanceDate ?: scheduleDate

        override fun getInstanceTime(domainFactory: DomainFactory): Time {
            val instanceJsonTime = instanceRecord.instanceJsonTime

            return if (instanceJsonTime == null) {
                getScheduleTime(domainFactory)
            } else {
                when (instanceJsonTime) {
                    is JsonTime.Custom -> getCustomTime(instanceJsonTime.id)
                    is JsonTime.Normal -> NormalTime(instanceJsonTime.hourMinute)
                }
            }
        }

        override val done get() = instanceRecord.done
    }

    class VirtualInstanceData<T, U, V : InstanceRecord<U>>(val taskId: T, val scheduleDateTime: DateTime) : InstanceData<T, U, V>() {

        override val scheduleDate by lazy { scheduleDateTime.date }

        override fun getScheduleTime(domainFactory: DomainFactory) = scheduleDateTime.time

        override val instanceDate by lazy { scheduleDate }

        override fun getInstanceTime(domainFactory: DomainFactory) = getScheduleTime(domainFactory)

        override val done: Long? = null
    }

    class InconsistentInstanceException(message: String) : Exception(message)
}

