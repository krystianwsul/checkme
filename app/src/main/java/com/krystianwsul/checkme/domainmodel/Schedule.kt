package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.*


abstract class Schedule(protected val domainFactory: DomainFactory) {

    protected abstract val scheduleBridge: ScheduleBridge

    protected val startExactTimeStamp by lazy { ExactTimeStamp(scheduleBridge.startTime) }

    val startTime by lazy { scheduleBridge.startTime }

    protected fun getEndExactTimeStamp() = scheduleBridge.endTime?.let { ExactTimeStamp(it) }

    val endTime get() = scheduleBridge.endTime

    val customTimeKey get() = scheduleBridge.customTimeKey

    val remoteCustomTimeKey get() = scheduleBridge.remoteCustomTimeKey

    abstract val scheduleType: ScheduleType

    val timePair
        get() = scheduleBridge.run {
            customTimeKey?.let { TimePair(it) } ?: TimePair(HourMinute(hour!!, minute!!))
        }

    protected val time
        get() = scheduleBridge.run {
            customTimeKey?.let { domainFactory.getCustomTime(it) }
                    ?: NormalTime(hour!!, minute!!)
        }

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        check(current(endExactTimeStamp))

        scheduleBridge.endTime = endExactTimeStamp.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        scheduleBridge.endTime = null
    }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val startExactTimeStamp = startExactTimeStamp
        val endExactTimeStamp = getEndExactTimeStamp()

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    abstract fun getInstances(task: Task, givenStartExactTimeStamp: ExactTimeStamp?, givenExactEndTimeStamp: ExactTimeStamp): List<Instance>

    abstract fun isVisible(task: Task, now: ExactTimeStamp, hack24: Boolean): Boolean

    abstract fun getScheduleText(): String

    abstract fun getNextAlarm(now: ExactTimeStamp): TimeStamp?

    fun delete() {
        domainFactory.getTaskForce(scheduleBridge.rootTaskKey).deleteSchedule(this)
        scheduleBridge.delete()
    }

    val scheduleId get() = scheduleBridge.scheduleId
}
