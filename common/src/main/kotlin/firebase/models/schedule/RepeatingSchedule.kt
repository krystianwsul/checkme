package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.RepeatingScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.invalidatableLazy
import com.krystianwsul.common.utils.invalidatableLazyCallbacks
import com.soywiz.klock.days
import firebase.models.schedule.generators.DateTimeSequenceGenerator

sealed class RepeatingSchedule(topLevelTask: Task) : Schedule(topLevelTask) {

    protected abstract val repeatingScheduleRecord: RepeatingScheduleRecord

    val from get() = repeatingScheduleRecord.from
    val until get() = repeatingScheduleRecord.until

    private val oldestVisibleDateProperty = invalidatableLazy {
        repeatingScheduleRecord.oldestVisible?.let { Date.fromJson(it) }
    }

    private val oldestVisibleDate by oldestVisibleDateProperty

    override val oldestVisible
        get() = oldestVisibleDate?.let { OldestVisible.RepeatingNonNull(it) } ?: OldestVisible.RepeatingNull

    private val intrinsicStartExactTimeStampProperty = invalidatableLazy {
        listOfNotNull(
                startExactTimeStampOffset,
                repeatingScheduleRecord.from?.let {
                    ExactTimeStamp.Local(it, HourMilli(0, 0, 0, 0))
                },
        ).maxOrNull()!!
    }.apply { addTo(startExactTimeStampOffsetProperty) }
    private val intrinsicStartExactTimeStamp by intrinsicStartExactTimeStampProperty

    val intrinsicEndExactTimeStamp by invalidatableLazyCallbacks {
        listOfNotNull(
            endExactTimeStampOffset,
            repeatingScheduleRecord.until
                ?.let { DateTime(it, HourMinute(0, 0)) }
                ?.toDateTimeSoy()
                ?.plus(1.days)
                ?.let(ExactTimeStamp::Local),
        ).minOrNull()
    }.apply {
        addTo(endExactTimeStampOffsetProperty)
    }

    protected abstract val dateTimeSequenceGenerator: DateTimeSequenceGenerator

    override fun getDateTimesInRange(
        scheduleInterval: ScheduleInterval,
        givenStartExactTimeStamp: ExactTimeStamp.Offset?,
        givenEndExactTimeStamp: ExactTimeStamp.Offset?,
        originalDateTime: Boolean,
        checkOldestVisible: Boolean,
    ): Sequence<DateTime> {
        val startExactTimeStamp = listOfNotNull(
            intrinsicStartExactTimeStamp,
                givenStartExactTimeStamp,
                oldestVisibleDate?.takeIf { checkOldestVisible }?.let {
                    ExactTimeStamp.Local(it, HourMilli(0, 0, 0, 0))
                },
                scheduleInterval.startExactTimeStampOffset,
        ).maxOrNull()!!

        val endExactTimeStamp = listOfNotNull(
            intrinsicEndExactTimeStamp,
            givenEndExactTimeStamp,
            scheduleInterval.endExactTimeStampOffset,
        ).minOrNull()

        if (endExactTimeStamp?.let { it <= startExactTimeStamp } == true) return emptySequence()

        return dateTimeSequenceGenerator.generate(startExactTimeStamp, endExactTimeStamp, time)
    }

    override fun isAfterOldestVisible(exactTimeStamp: ExactTimeStamp): Boolean {
        return oldestVisibleDate?.let {
            ExactTimeStamp.Local(it, HourMilli(0, 0, 0, 0)) <= exactTimeStamp
        } ?: true
    }

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval, now: ExactTimeStamp.Local) {
        val dateTimes = getDateTimesInRange(
            scheduleInterval,
                null,
                now.toOffset().plusOne(),
        ).toList()

        val pastRootInstances = dateTimes.map(topLevelTask::getInstance).filter { it.isRootInstance() }

        val oldestVisible = listOf(
                pastRootInstances.filter {
                    !it.exists() && it.isVisible(
                            now,
                            Instance.VisibilityOptions(hack24 = true, assumeRoot = true)
                    )
                }
                        .map { it.scheduleDate }
                        .toList(),
                listOf(now.date)
        ).flatten().minOrNull()!!

        repeatingScheduleRecord.oldestVisible = oldestVisible.toJson()
        oldestVisibleDateProperty.invalidate()
    }
}
