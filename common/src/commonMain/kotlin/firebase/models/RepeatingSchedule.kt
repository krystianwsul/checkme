package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.RepeatingScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceSequenceData
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
        get() = oldestVisibleDate?.let {
            OldestVisible.RepeatingNonNull(it)
        } ?: OldestVisible.RepeatingNull

    override fun getDateTimesInRange(
            scheduleInterval: ScheduleInterval<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): InstanceSequenceData {
        val startExactTimeStamp = listOfNotNull(
                startExactTimeStamp,
                repeatingScheduleRecord.from
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toExactTimeStamp(),
                givenStartExactTimeStamp,
                oldestVisibleDate?.let { ExactTimeStamp(it, HourMilli(0, 0, 0, 0)) },
                scheduleInterval.startExactTimeStamp
        ).maxOrNull()!!

        val intrinsicEndExactTimeStamp = listOfNotNull(
                endExactTimeStamp,
                repeatingScheduleRecord.until
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toDateTimeSoy()
                        ?.plus(1.days)
                        ?.let { ExactTimeStamp(it) },
                scheduleInterval.endExactTimeStamp
        ).minOrNull()

        val endExactTimeStamp = listOfNotNull(
                intrinsicEndExactTimeStamp,
                givenExactEndTimeStamp
        ).minOrNull()

        if (endExactTimeStamp?.let { it <= startExactTimeStamp } == true) {
            val hasMore = when {
                givenExactEndTimeStamp != null && intrinsicEndExactTimeStamp != null -> intrinsicEndExactTimeStamp > givenExactEndTimeStamp
                givenExactEndTimeStamp != null -> true
                else -> false // intrinsicEndExactTimeStamp != null
            }

            return InstanceSequenceData(emptySequence(), hasMore)
        }

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

            var loopStartCalendar = startExactTimeStamp.date.toDateTimeTz() + 1.days
            val loopEndCalendar = endExactTimeStamp?.date?.toDateTimeTz()

            val calendarSequence = generateSequence {
                if (loopEndCalendar?.let { it <= loopStartCalendar } == true)
                    return@generateSequence null

                val date = Date(loopStartCalendar)
                loopStartCalendar += 1.days

                NullableWrapper(getDateTimeInDate(date, null, null))
            }

            val endSequence = listOfNotNull(endExactTimeStamp?.let { getDateTimeInDate(it.date, null, it.hourMilli) }).asSequence()

            nullableSequence = startSequence + calendarSequence.map { it.value } + endSequence
        }

        val hasMore = if (givenExactEndTimeStamp != null) {
            intrinsicEndExactTimeStamp?.let { it > givenExactEndTimeStamp } != false
        } else {
            null // meaningless for open sequence
        }

        return InstanceSequenceData(nullableSequence.filterNotNull(), hasMore)
    }

    private fun getDateTimeInDate(
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
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

    override fun isVisible(
            scheduleInterval: ScheduleInterval<T>,
            task: Task<T>,
            now: ExactTimeStamp,
            hack24: Boolean
    ): Boolean {
        scheduleInterval.requireCurrent(now)
        requireCurrent(now)

        return until?.let {
            getDateTimesInRange(
                    scheduleInterval,
                    null,
                    null
            ).dateTimes.any { task.getInstance(it).isVisible(now, hack24) }
        } ?: true
    }

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval<T>, now: ExactTimeStamp) {
        val pastRootInstances = getDateTimesInRange(
                scheduleInterval,
                null,
                now.plusOne()
        ).dateTimes
                .map(rootTask::getInstance)
                .filter { it.isRootInstance(now) }

        val oldestVisible = listOf(
                pastRootInstances.filter { it.isVisible(now, true) && !it.exists() }
                        .map { it.scheduleDate }
                        .toList(),
                listOf(now.date)
        ).flatten().minOrNull()!!

        repeatingScheduleRecord.oldestVisible = oldestVisible.toJson()
        oldestVisibleDateProperty.invalidate()
    }

    override fun matchesScheduleDateTimeHelper(scheduleDateTime: DateTime, checkOldestVisible: Boolean): Boolean {
        if (checkOldestVisible && oldestVisibleDate?.let { scheduleDateTime.date < it } == true) return false

        if (timePair != scheduleDateTime.time.timePair) return false

        if (from?.let { scheduleDateTime.date < it } == true) return false

        if (until?.let { scheduleDateTime.date > it } == true) return false

        return matchesScheduleDateRepeatingHelper(scheduleDateTime.date)
    }

    protected abstract fun matchesScheduleDateRepeatingHelper(scheduleDate: Date): Boolean
}
