package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.models.interval.ScheduleInterval
import com.krystianwsul.common.firebase.records.RepeatingScheduleRecord
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
        get() = oldestVisibleDate?.let {
            OldestVisible.RepeatingNonNull(it)
        } ?: OldestVisible.RepeatingNull

    override fun getDateTimesInRange(
            scheduleInterval: ScheduleInterval<T>,
            givenStartDateTime: DateTime?,
            givenEndDateTime: DateTime?
    ): Sequence<DateTime> {
        val startDateTime = listOfNotNull(
                startDateTime,
                repeatingScheduleRecord.from?.let { DateTime(it, HourMinute(0, 0)) },
                givenStartDateTime,
                oldestVisibleDate?.let { DateTime(it, HourMinute(0, 0)) },
                scheduleInterval.startExactTimeStamp.toDateTime()
        ).maxOrNull()!!

        val intrinsicEndDateTime = listOfNotNull(
                endDateTime,
                repeatingScheduleRecord.until
                        ?.let { DateTime(it, HourMinute(0, 0)) }
                        ?.toDateTimeSoy()
                        ?.plus(1.days)
                        ?.local
                        ?.let { DateTime(it) },
                scheduleInterval.endExactTimeStamp?.toDateTime()
        ).minOrNull()

        val endDateTime = listOfNotNull(
                intrinsicEndDateTime,
                givenEndDateTime
        ).minOrNull()

        if (endDateTime?.let { it <= startDateTime } == true) return emptySequence()

        val nullableSequence: Sequence<DateTime?>

        if (startDateTime.date == endDateTime?.date) {
            nullableSequence = sequenceOf(getDateTimeInDate(
                    startDateTime.date,
                    startDateTime.hourMinute,
                    endDateTime.hourMinute
            ))
        } else {
            val startSequence = sequenceOf(getDateTimeInDate(
                    startDateTime.date,
                    startDateTime.hourMinute,
                    null
            ))

            fun Date.toDateTimeSoy() = DateTimeSoy(year, month, day)
            fun DateTimeSoy.toDate() = Date(yearInt, month1, dayOfMonth)

            var loopStartCalendar = startDateTime.date.toDateTimeSoy() + 1.days

            val loopEndCalendar = endDateTime?.date?.toDateTimeSoy()

            val calendarSequence = generateSequence {
                if (loopEndCalendar?.let { it <= loopStartCalendar } == true)
                    return@generateSequence null

                val date = loopStartCalendar.toDate()
                loopStartCalendar += 1.days

                NullableWrapper(getDateTimeInDate(date, null, null))
            }

            val endSequence = listOfNotNull(
                    endDateTime?.let { getDateTimeInDate(it.date, null, it.hourMinute) }
            ).asSequence()

            nullableSequence = startSequence + calendarSequence.map { it.value } + endSequence
        }

        return nullableSequence.filterNotNull()
    }

    private fun getDateTimeInDate(
            date: Date,
            startHourMinute: HourMinute?,
            endHourMinute: HourMinute?
    ): DateTime? {
        if (!hasInstanceInDate(date, startHourMinute, endHourMinute)) return null

        return DateTime(date, time)
    }

    private fun hasInstanceInDate(date: Date, startHourMinute: HourMinute?, endHourMinute: HourMinute?): Boolean {
        if (!containsDate(date)) return false

        val hourMinute by lazy { time.getHourMinute(date.dayOfWeek) }

        if (startHourMinute != null && startHourMinute > hourMinute) return false
        if (endHourMinute != null && endHourMinute <= hourMinute) return false

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
            ).any { task.getInstance(it).isVisible(now, hack24) }
        } ?: true
    }

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval<T>, now: ExactTimeStamp) {
        val dateTimes = getDateTimesInRange(
                scheduleInterval,
                null,
                now.toDateTime().plusOneMinute()
        ).toList()

        // this filtering shouldn't be necessary
        val pastRootInstances = dateTimes.map(rootTask::getInstance).filter { it.isRootInstance(now) }

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
