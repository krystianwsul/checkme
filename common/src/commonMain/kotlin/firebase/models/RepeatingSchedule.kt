package com.krystianwsul.common.firebase.models


import com.krystianwsul.common.firebase.records.RepeatingScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.NullableWrapper
import com.krystianwsul.common.utils.ProjectType
import com.soywiz.klock.days

abstract class RepeatingSchedule<T : ProjectType>(rootTask: Task<T>) : Schedule<T>(rootTask) {

    protected abstract val repeatingScheduleRecord: RepeatingScheduleRecord<T>

    val from get() = repeatingScheduleRecord.from
    val until get() = repeatingScheduleRecord.until

    override val oldestVisible get() = repeatingScheduleRecord.oldestVisible?.let { Date.fromJson(it) }

    override fun <T : ProjectType> getInstances(
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Pair<Sequence<Instance<T>>, Boolean> {
        val startExactTimeStamp = listOfNotNull(
                startExactTimeStamp,
                repeatingScheduleRecord.from
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toExactTimeStamp(),
                givenStartExactTimeStamp,
                oldestVisible?.let { ExactTimeStamp(it, HourMilli(0, 0, 0, 0)) }
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                endExactTimeStamp,
                repeatingScheduleRecord.until
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toDateTimeSoy()
                        ?.plus(1.days)
                        ?.let { ExactTimeStamp(it) },
                givenExactEndTimeStamp
        ).min()

        if (endExactTimeStamp?.let { it <= startExactTimeStamp } == true)
            return Pair(emptySequence(), false)

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

        return Pair(nullableSequence.filterNotNull(), endExactTimeStamp != null)
    }

    protected abstract fun <T : ProjectType> getInstanceInDate(
            task: Task<T>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<T>?

    override fun isVisible(task: Task<*>, now: ExactTimeStamp, hack24: Boolean): Boolean {
        requireCurrent(now)

        return until?.let {
            getInstances(task, null, null).first.any { it.isVisible(now, hack24) }
        } ?: true
    }

    override fun updateOldestVisible(now: ExactTimeStamp) {
        val pastRootInstances = getInstances(
                rootTask,
                null,
                now.plusOne()
        ).first.filter { it.isRootInstance(now) }

        val oldestVisible = listOf(
                pastRootInstances.filter { it.isVisible(now, true) && !it.exists() }
                        .map { it.scheduleDate }
                        .toList(),
                listOf(now.date)
        ).flatten().min()!!

        repeatingScheduleRecord.oldestVisible = oldestVisible.toJson()
    }

    override fun matchesScheduleDateTimeHelper(scheduleDateTime: DateTime): Boolean {
        if (from?.let { scheduleDateTime.date < it } == true)
            return false

        if (until?.let { scheduleDateTime.date > it } == true)
            return false

        return matchesScheduleDateTimeRepeatingHelper(scheduleDateTime)
    }

    protected abstract fun matchesScheduleDateTimeRepeatingHelper(scheduleDateTime: DateTime): Boolean
}
