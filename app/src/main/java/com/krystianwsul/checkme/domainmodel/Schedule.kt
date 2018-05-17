package com.krystianwsul.checkme.domainmodel

import android.content.Context

import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.TimeStamp

import junit.framework.Assert

abstract class Schedule(protected val domainFactory: DomainFactory) {

    protected abstract val scheduleBridge: ScheduleBridge

    protected val startExactTimeStamp by lazy { ExactTimeStamp(scheduleBridge.startTime) }

    val startTime by lazy { scheduleBridge.startTime }

    protected fun getEndExactTimeStamp() = scheduleBridge.getEndTime()?.let { ExactTimeStamp(it) }

    val endTime get() = scheduleBridge.getEndTime()

    abstract val customTimeKey: CustomTimeKey?

    protected val remoteCustomTimeKey get() = scheduleBridge.remoteCustomTimeKey

    abstract val scheduleType: ScheduleType

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        Assert.assertTrue(current(endExactTimeStamp))

        scheduleBridge.setEndTime(endExactTimeStamp.long)
    }

    fun current(exactTimeStamp: ExactTimeStamp): Boolean {
        val startExactTimeStamp = startExactTimeStamp
        val endExactTimeStamp = getEndExactTimeStamp()

        return startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp == null || endExactTimeStamp > exactTimeStamp)
    }

    abstract fun getInstances(task: Task, givenStartExactTimeStamp: ExactTimeStamp?, givenExactEndTimeStamp: ExactTimeStamp): List<Instance>

    abstract fun isVisible(task: Task, now: ExactTimeStamp): Boolean

    abstract fun getScheduleText(context: Context): String

    abstract fun getNextAlarm(now: ExactTimeStamp): TimeStamp?

    fun delete() {
        domainFactory.getTaskForce(scheduleBridge.rootTaskKey).deleteSchedule(this)
        scheduleBridge.delete()
    }
}
