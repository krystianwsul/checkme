package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.IntervalBuilder
import com.krystianwsul.common.firebase.records.ScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleType


abstract class Schedule<T : ProjectType>(protected val rootTask: Task<T>) : Current {

    protected abstract val scheduleRecord: ScheduleRecord<T>

    override val startExactTimeStamp by lazy { ExactTimeStamp(scheduleRecord.startTime) }
    override val endExactTimeStamp get() = scheduleRecord.endTime?.let { ExactTimeStamp(it) }

    val startTime by lazy { scheduleRecord.startTime }

    val endTime get() = scheduleRecord.endTime

    val customTimeKey get() = scheduleRecord.customTimeKey

    abstract val scheduleType: ScheduleType

    val timePair get() = scheduleRecord.timePair

    val time get() = timePair.toTime()

    protected fun TimePair.toTime() = customTimeKey
            ?.let { rootTask.project.getCustomTime(it.customTimeId) }
            ?: Time.Normal(hourMinute!!)

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp) {
        requireCurrent(endExactTimeStamp)

        scheduleRecord.endTime = endExactTimeStamp.long

        rootTask.invalidateIntervals()
    }

    fun clearEndExactTimeStamp(now: ExactTimeStamp) {
        requireNotCurrent(now)

        scheduleRecord.endTime = null

        rootTask.invalidateIntervals()
    }

    abstract fun getInstances(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Pair<Sequence<Instance<T>>, Boolean?> // second parameter required if end not null

    abstract fun isVisible(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            task: Task<T>,
            now: ExactTimeStamp,
            hack24: Boolean
    ): Boolean

    abstract fun getNextAlarm(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            now: ExactTimeStamp
    ): TimeStamp?

    fun delete() {
        rootTask.deleteSchedule(this)
        scheduleRecord.delete()
    }

    val scheduleId get() = scheduleRecord.scheduleId

    abstract val oldestVisible: Date?

    abstract fun updateOldestVisible(scheduleInterval: IntervalBuilder.ScheduleInterval<T>, now: ExactTimeStamp)

    fun matchesScheduleDateTime(
            scheduleInterval: IntervalBuilder.ScheduleInterval<T>,
            scheduleDateTime: DateTime
    ): Boolean {
        val exactTimeStamp = scheduleDateTime.toExactTimeStamp()

        if (exactTimeStamp < scheduleInterval.startExactTimeStamp)
            return false

        if (scheduleInterval.endExactTimeStamp?.let { exactTimeStamp >= it } == true)
            return false

        if (oldestVisible?.let { scheduleDateTime.date < it } == true)
            return false

        if (timePair != scheduleDateTime.time.timePair)
            return false

        return matchesScheduleDateTimeHelper(scheduleDateTime)
    }

    protected abstract fun matchesScheduleDateTimeHelper(scheduleDateTime: DateTime): Boolean
}
