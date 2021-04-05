package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.schedule.RepeatingScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.NullableWrapper
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.invalidatableLazy
import com.soywiz.klock.days

abstract class RepeatingSchedule<T : ProjectType>(rootTask: Task<T>) : Schedule<T>(rootTask) {

    protected abstract val repeatingScheduleRecord: RepeatingScheduleRecord<T>

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
            scheduleInterval: ScheduleInterval<T>,
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

        val nullableSequence: Sequence<DateTime?>

        if (startExactTimeStamp.date == endExactTimeStamp?.date) {
            nullableSequence = sequenceOf(getDateTimeInDate(
                    startExactTimeStamp.date,
                    startExactTimeStamp.hourMilli,
                    endExactTimeStamp.hourMilli
            ))
        } else {
            val startSequence = sequenceOf(getDateTimeInDate(
                    startExactTimeStamp.date,
                    startExactTimeStamp.hourMilli,
                    null
            ))

            fun Date.toDateTimeSoy() = DateTimeSoy(year, month, day)
            fun DateTimeSoy.toDate() = Date(yearInt, month1, dayOfMonth)

            var loopStartCalendar = startExactTimeStamp.date.toDateTimeSoy() + 1.days

            val loopEndCalendar = endExactTimeStamp?.date?.toDateTimeSoy()

            val calendarSequence = generateSequence {
                if (loopEndCalendar?.let { it <= loopStartCalendar } == true)
                    return@generateSequence null

                val date = loopStartCalendar.toDate()
                loopStartCalendar += 1.days

                NullableWrapper(getDateTimeInDate(date, null, null))
            }

            val endSequence = listOfNotNull(
                    endExactTimeStamp?.let { getDateTimeInDate(it.date, null, it.hourMilli) }
            ).asSequence()

            nullableSequence = startSequence + calendarSequence.map { it.value } + endSequence
        }

        return nullableSequence.filterNotNull()
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

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval<T>, now: ExactTimeStamp.Local) {
        val dateTimes = getDateTimesInRange(
                scheduleInterval,
                null,
                now.toOffset().plusOne()
        ).toList()

        val pastRootInstances = dateTimes.map(rootTask::getInstance).filter { it.isRootInstance() }

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
