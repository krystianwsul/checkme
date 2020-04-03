package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType


abstract class Schedule<T : ProjectType>(private val rootTask: Task<T>) : Current {

    protected abstract val scheduleBridge: ScheduleBridge<T>

    override val startExactTimeStamp by lazy { ExactTimeStamp(scheduleBridge.startTime) }
    override val endExactTimeStamp get() = scheduleBridge.endTime?.let { ExactTimeStamp(it) }

    val startTime by lazy { scheduleBridge.startTime }

    val endTime get() = scheduleBridge.endTime

    val customTimeKey get() = scheduleBridge.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleBridge.timePair

    val time
        get() = customTimeKey?.let {
            rootTask.remoteProject.getRemoteCustomTime(it.customTimeId)
        } ?: Time.Normal(timePair.hourMinute!!)


    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrent(endExactTimeStamp)

        scheduleBridge.endTime = endExactTimeStamp.long
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        scheduleBridge.endTime = null
    }

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
