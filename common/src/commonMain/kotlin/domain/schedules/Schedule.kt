package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.NormalTime
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleType


abstract class Schedule<T : RemoteCustomTimeId, U : ProjectKey>(private val rootTask: Task<T, U>) {

    protected abstract val scheduleBridge: ScheduleBridge<T, U>

    protected val startExactTimeStamp by lazy { ExactTimeStamp(scheduleBridge.startTime) }

    val startTime by lazy { scheduleBridge.startTime }

    protected fun getEndExactTimeStamp() = scheduleBridge.endTime?.let { ExactTimeStamp(it) }

    val endTime get() = scheduleBridge.endTime

    val customTimeKey get() = scheduleBridge.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleBridge.timePair

    val time
        get() = timePair.run {
            customTimeKey?.let {
                rootTask.remoteProject.getRemoteCustomTime(it.remoteCustomTimeId)
            } ?: NormalTime(hourMinute!!)
        }

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        check(current(endExactTimeStamp))

        scheduleBridge.endTime = endExactTimeStamp.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        check(!current(now))

        scheduleBridge.endTime = null
    }

    fun current(exactTimeStamp: ExactTimeStamp) =
            startExactTimeStamp <= exactTimeStamp && (getEndExactTimeStamp()?.let { it > exactTimeStamp } != false)

    abstract fun <T : RemoteCustomTimeId, U : ProjectKey> getInstances(
            task: Task<T, U>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Sequence<Instance<T, U>>

    abstract fun isVisible(task: Task<*, *>, now: ExactTimeStamp, hack24: Boolean): Boolean

    abstract fun getNextAlarm(now: ExactTimeStamp): TimeStamp?

    fun delete() {
        rootTask.deleteSchedule(this)
        scheduleBridge.delete()
    }

    val scheduleId get() = scheduleBridge.scheduleId
}
