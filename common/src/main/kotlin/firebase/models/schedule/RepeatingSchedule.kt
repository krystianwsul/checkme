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

    private val repeatingOldestVisibleProperty = invalidatableLazy {
        RepeatingOldestVisible.fromJson(repeatingScheduleRecord.oldestVisible)
    }

    private val repeatingOldestVisible by repeatingOldestVisibleProperty

    override val oldestVisible get() = OldestVisible.Repeating(repeatingOldestVisible)

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
        val oldestVisibleExactTimeStamp = if (checkOldestVisible) {
            when (val repeatingOldestVisible = repeatingOldestVisible) {
                is RepeatingOldestVisible.Ongoing -> repeatingOldestVisible.date?.toMidnightExactTimeStamp()
                is RepeatingOldestVisible.Ended -> return emptySequence()
            }
        } else {
            null
        }

        val startExactTimeStamp = listOfNotNull(
            intrinsicStartExactTimeStamp,
            givenStartExactTimeStamp,
            oldestVisibleExactTimeStamp,
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

    override fun matchesScheduleDate(scheduleDate: Date) = repeatingOldestVisible.matchesScheduleDate(scheduleDate)

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval, now: ExactTimeStamp.Local) {
        val oldestVisibleDate = getDateTimesInRange(
            scheduleInterval,
            null,
            null,
        ).map(topLevelTask::getInstance)
            .filter { !it.exists() }
            .filter { it.isRootInstance() }
            .filter { it.isVisible(now, Instance.VisibilityOptions(hack24 = true, assumeRoot = true)) }
            .firstOrNull()
            ?.scheduleDate

        val oldestVisibleCompat = oldestVisibleDate ?: intrinsicEndExactTimeStamp!!.date + 1.days
        repeatingScheduleRecord.oldestVisibleCompat = oldestVisibleCompat.toJson()

        val oldestVisible = oldestVisibleDate?.let(RepeatingOldestVisible::Present) ?: RepeatingOldestVisible.Ended
        repeatingScheduleRecord.oldestVisible = oldestVisible.toJson()

        repeatingOldestVisibleProperty.invalidate()
    }

    sealed interface RepeatingOldestVisible {

        companion object {

            fun fromJson(json: String?) = when (json) {
                null -> None
                Ended.JSON_VALUE -> Ended
                else -> Present(Date.fromJson(json))
            }
        }

        fun matchesScheduleDate(scheduleDate: Date): Boolean

        sealed interface Ongoing : RepeatingOldestVisible {

            val date: Date?
        }

        sealed interface Set : RepeatingOldestVisible {

            fun toJson(): String
        }

        object None : Ongoing {

            override val date: Date? = null

            override fun matchesScheduleDate(scheduleDate: Date) = true
        }

        data class Present(override val date: Date) : Ongoing, Set {

            override fun toJson() = date.toJson()

            override fun matchesScheduleDate(scheduleDate: Date) = date <= scheduleDate
        }

        object Ended : RepeatingOldestVisible, Set {

            const val JSON_VALUE = "ended"

            override fun toJson() = JSON_VALUE

            override fun matchesScheduleDate(scheduleDate: Date) = false
        }
    }
}
