package com.krystianwsul.common.domain.schedules


import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.NullableWrapper
import com.soywiz.klock.days

abstract class RepeatingSchedule(rootTask: Task<*, *>) : Schedule(rootTask) {

    protected abstract val repeatingScheduleBridge: RepeatingScheduleBridge

    val from get() = repeatingScheduleBridge.from
    val until get() = repeatingScheduleBridge.until

    override fun getInstances(
            task: Task<*, *>,
            givenStartExactTimeStamp: ExactTimeStamp?,
            givenExactEndTimeStamp: ExactTimeStamp? // can be null only if until or endExactTimeStamp are set
    ): Sequence<Instance<*, *>> {
        val startExactTimeStamp = listOfNotNull(
                startExactTimeStamp,
                repeatingScheduleBridge.from
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toExactTimeStamp(),
                givenStartExactTimeStamp
        ).max()!!

        val endExactTimeStamp = listOfNotNull(
                getEndExactTimeStamp(),
                repeatingScheduleBridge.until
                        ?.let { TimeStamp(it, HourMinute(0, 0)) }
                        ?.toDateTimeSoy()
                        ?.plus(1.days)
                        ?.let { ExactTimeStamp(it) },
                givenExactEndTimeStamp
        ).min()!! // todo from delete task after until

        if (startExactTimeStamp >= endExactTimeStamp)
            return emptySequence()

        check(startExactTimeStamp < endExactTimeStamp)

        val nullableSequence: Sequence<Instance<*, *>?>

        if (startExactTimeStamp.date == endExactTimeStamp.date) {
            nullableSequence = sequenceOf(getInstanceInDate(task, startExactTimeStamp.date, startExactTimeStamp.hourMilli, endExactTimeStamp.hourMilli))
        } else {
            val startSequence = sequenceOf(getInstanceInDate(task, startExactTimeStamp.date, startExactTimeStamp.hourMilli, null))

            var loopStartCalendar = startExactTimeStamp.date.toDateTimeTz() + 1.days
            val loopEndCalendar = endExactTimeStamp.date.toDateTimeTz()

            val calendarSequence = generateSequence {
                if (loopStartCalendar < loopEndCalendar) {
                    val date = Date(loopStartCalendar)
                    loopStartCalendar += 1.days

                    NullableWrapper(getInstanceInDate(task, date, null, null))
                } else {
                    null
                }
            }

            val endSequence = sequenceOf(getInstanceInDate(task, endExactTimeStamp.date, null, endExactTimeStamp.hourMilli))

            nullableSequence = startSequence + calendarSequence.map { it.value } + endSequence
        }

        return nullableSequence.filterNotNull()
    }

    protected abstract fun getInstanceInDate(
            task: Task<*, *>,
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?
    ): Instance<*, *>?

    override fun isVisible(task: Task<*, *>, now: ExactTimeStamp, hack24: Boolean): Boolean {
        check(current(now))

        return until?.let {
            getInstances(task, null, null).any { it.isVisible(now, hack24) }
        } ?: true
    }
}
