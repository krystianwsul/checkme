package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.NullableWrapper
import com.krystianwsul.common.utils.ProjectType
import com.soywiz.klock.days

abstract class RepeatingSchedule<T : ProjectType>(rootTask: Task<T>) : Schedule<T>(rootTask) {

    protected abstract val repeatingScheduleBridge: RepeatingScheduleBridge<T>

    val from get() = repeatingScheduleBridge.from
    val until get() = repeatingScheduleBridge.until

    override fun <T : ProjectType> getInstances(
            task: Task<T>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp?
    ): Sequence<Instance<T>> {
        val startExactTimeStamp = listOfNotNull(
                startExactTimeStamp,
                repeatingScheduleBridge.from
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toExactTimeStamp(),
                givenStartExactTimeStamp
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                endExactTimeStamp,
                repeatingScheduleBridge.until
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toDateTimeSoy()
                        ?.plus(1.days)
                        ?.let { ExactTimeStamp(it) },
                givenExactEndTimeStamp
        ).min()

        if (endExactTimeStamp?.let { it <= startExactTimeStamp } == true)
            return emptySequence()

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

        return nullableSequence.filterNotNull()
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
            getInstances(task, null, null).any { it.isVisible(now, hack24) }
        } ?: true
    }
}
