package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType


abstract class Schedule<T : ProjectType>(private val rootTask: Task<T>) {

    protected abstract val scheduleBridge: ScheduleBridge<T>

    protected val startExactTimeStamp by lazy { ExactTimeStamp(scheduleBridge.startTime) }

    val startTime by lazy { scheduleBridge.startTime }

    protected val endExactTimeStamp get() = scheduleBridge.endTime?.let { ExactTimeStamp(it) }

    val endTime get() = scheduleBridge.endTime

    val customTimeKey get() = scheduleBridge.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleBridge.timePair

    val time
        get() = customTimeKey?.let {
            rootTask.remoteProject.getRemoteCustomTime(it.customTimeId)
        } ?: Time.Normal(timePair.hourMinute!!)


    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        check(current(endExactTimeStamp))

        scheduleBridge.endTime = endExactTimeStamp.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        scheduleBridge.endTime = null
    }

    fun current(exactTimeStamp: ExactTimeStamp) =
            startExactTimeStamp <= exactTimeStamp && (endExactTimeStamp?.let { it > exactTimeStamp } != false)

    abstract fun <T : ProjectType> getInstances(
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Sequence<Instance<T>>

    abstract fun isVisible(task: Task<*>, now: ExactTimeStamp, hack24: Boolean): Boolean

    abstract fun getNextAlarm(now: ExactTimeStamp): TimeStamp?

    fun delete() {
        rootTask.deleteSchedule(this)
        scheduleBridge.delete()
    }

    val scheduleId get() = scheduleBridge.scheduleId
}
