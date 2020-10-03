package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.RepeatingScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceSequenceData
import com.krystianwsul.common.utils.NullableWrapper
import com.krystianwsul.common.utils.ProjectType
import com.soywiz.klock.days
import firebase.models.interval.ScheduleInterval

abstract class RepeatingSchedule<T : ProjectType>(rootTask: Task<T>) : Schedule<T>(rootTask) {

    protected abstract val repeatingScheduleRecord: RepeatingScheduleRecord<T>

    val from get() = repeatingScheduleRecord.from
    val until get() = repeatingScheduleRecord.until

    private val oldestVisibleDate get() = repeatingScheduleRecord.oldestVisible?.let { Date.fromJson(it) }

    override val oldestVisible
        get() = oldestVisibleDate?.let {
            OldestVisible.RepeatingNonNull(it)
        } ?: OldestVisible.RepeatingNull

    override fun getInstances(
            scheduleInterval: ScheduleInterval<T>,
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): InstanceSequenceData<T> {
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

        val nullableSequence: Sequence<Instance<*>?>

        if (startExactTimeStamp.date == endExactTimeStamp?.date) {
            nullableSequence = sequenceOf(getInstanceInDate(
                    task,
                    startExactTimeStamp.date,
                    startExactTimeStamp.hourMilli,
                    endExactTimeStamp.hourMilli
            ))
        } else {
            val startSequence = sequenceOf(getInstanceInDate(
                    task,
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

                NullableWrapper(getInstanceInDate(task, date, null, null))
            }

            val endSequence = listOfNotNull(endExactTimeStamp?.let { getInstanceInDate(task, it.date, null, it.hourMilli) }).asSequence()

            nullableSequence = startSequence + calendarSequence.map { it.value } + endSequence
        }

        val hasMore = if (givenExactEndTimeStamp != null) {
            intrinsicEndExactTimeStamp?.let { it > givenExactEndTimeStamp } != false
        } else {
            null // meaningless for open sequence
        }

        return InstanceSequenceData(nullableSequence.filterNotNull(), hasMore)
    }

    protected abstract fun <T : ProjectType> getInstanceInDate(
            task: Task<T>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T>?

    override fun isVisible(
            scheduleInterval: ScheduleInterval<T>,
            task: Task<T>,
            now: ExactTimeStamp,
            hack24: Boolean
    ): Boolean {
        scheduleInterval.requireCurrent(now)
        requireCurrent(now)

        return until?.let {
            getInstances(
                    scheduleInterval,
                    task,
                    null,
                    null
            ).instances.any { it.isVisible(now, hack24) }
        } ?: true
    }

    override fun updateOldestVisible(scheduleInterval: ScheduleInterval<T>, now: ExactTimeStamp) {
        val pastRootInstances = getInstances(
                scheduleInterval,
                rootTask,
                null,
                now.plusOne()
        ).instances.filter { it.isRootInstance(now) }

        val oldestVisible = listOf(
                pastRootInstances.filter { it.isVisible(now, true) && !it.exists() }
                        .map { it.scheduleDate }
                        .toList(),
                listOf(now.date)
        ).flatten().minOrNull()!!

        repeatingScheduleRecord.oldestVisible = oldestVisible.toJson()
    }

    override fun matchesScheduleDateTimeHelper(scheduleDateTime: DateTime, checkOldestVisible: Boolean): Boolean {
        if (checkOldestVisible && oldestVisibleDate?.let { scheduleDateTime.date < it } == true)
            return false

        if (timePair != scheduleDateTime.time.timePair)
            return false

        if (from?.let { scheduleDateTime.date < it } == true)
            return false

        if (until?.let { scheduleDateTime.date > it } == true)
            return false

        return matchesScheduleDateTimeRepeatingHelper(scheduleDateTime)
    }

    protected abstract fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime): Boolean
}
