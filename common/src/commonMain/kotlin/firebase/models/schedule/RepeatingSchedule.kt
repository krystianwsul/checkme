package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.schedule.RepeatingScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.invalidatableLazy
import com.soywiz.klock.days
import com.soywiz.klock.plus

abstract class RepeatingSchedule(topLevelTask: Task) : Schedule(topLevelTask) {

    protected abstract val repeatingScheduleRecord: RepeatingScheduleRecord

    val from get() = repeatingScheduleRecord.from
    val until get() = repeatingScheduleRecord.until

    private val oldestVisibleDateProperty = invalidatableLazy {
        repeatingScheduleRecord.oldestVisible?.let { Date.fromJson(it) }
    }

    private val oldestVisibleDate by oldestVisibleDateProperty

    override val oldestVisible
        get() = oldestVisibleDate?.let { OldestVisible.RepeatingNonNull(it) } ?: OldestVisible.RepeatingNull

    private val intrinsicStartExactTimeStamp by lazy {
        listOfNotNull(
                startExactTimeStampOffset,
                repeatingScheduleRecord.from?.let {
                    ExactTimeStamp.Local(it, HourMilli(0, 0, 0, 0))
                },
        ).maxOrNull()!!
    }

    private val intrinsicEndExactTimeStamp by lazy {
        listOfNotNull(
                endExactTimeStampOffset,
                repeatingScheduleRecord.until
                        ?.let { DateTime(it, HourMinute(0, 0)) }
                        ?.toDateTimeSoy()
                        ?.plus(1.days)
                        ?.let(ExactTimeStamp::Local),
        ).minOrNull()
    }

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

        fun DateSoy.toDate() = Date(year, month1, day)

        val startSoyDate = startExactTimeStamp.date.toDateSoy()
        var currentSoyDate = startSoyDate

        val endSoyDate = endExactTimeStamp?.date?.toDateSoy()

        // ridiculous optimizations
        return generateSequence {
            var endHourMilli: HourMilli? = null
            if (endSoyDate != null) {
                val comparison = currentSoyDate.compareTo(endSoyDate)
                if (comparison > 0) { // passed the end
                    return@generateSequence null
                } else if (comparison == 0) { // last day
                    endHourMilli = endExactTimeStamp.hourMilli
                }
            }

            // first day
            val startHourMilli = if (startSoyDate == currentSoyDate) startExactTimeStamp.hourMilli else null

            val date = currentSoyDate.toDate()
            currentSoyDate += 1.days

            getDateTimeInDate(date, startHourMilli, endHourMilli) ?: Unit
        }.filterIsInstance<DateTime>()
    }

    override fun isAfterOldestVisible(exactTimeStamp: ExactTimeStamp): Boolean {
        return oldestVisibleDate?.let {
            ExactTimeStamp.Local(it, HourMilli(0, 0, 0, 0)) <= exactTimeStamp
        } ?: true
    }

    private fun getDateTimeInDate(
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?,
    ): DateTime? {
        if (!hasInstanceInDate(date, startHourMilli, endHourMilli)) return null

        return DateTime(date, time)
    }

    private fun hasInstanceInDate(date: Date, startHourMilli: HourMilli?, endHourMilli: HourMilli?): Boolean {
        if (!containsDate(date)) return false

        val hourMilli by lazy { time.getHourMinute(date.dayOfWeek).toHourMilli() }

        if (startHourMilli != null && startHourMilli > hourMilli) return false
        if (endHourMilli != null && endHourMilli <= hourMilli) return false

        return true
    }

    protected abstract fun containsDate(date: Date): Boolean

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
