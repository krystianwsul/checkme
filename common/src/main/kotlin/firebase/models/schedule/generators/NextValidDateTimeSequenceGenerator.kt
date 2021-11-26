package firebase.models.schedule.generators

import com.krystianwsul.common.time.*
import com.soywiz.klock.days
import com.soywiz.klock.plus

abstract class NextValidDateTimeSequenceGenerator : DateTimeSequenceGenerator {

    private fun getNextValidDate(startDateSoy: DateSoy) = getNextValidDateHelper(startDateSoy).also {
        check(containsDateSoy(it)) // todo sequence checks
    }

    protected abstract fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy

    protected abstract fun containsDateSoy(dateSoy: DateSoy): Boolean // todo sequence checks

    override fun generate(
        startExactTimeStamp: ExactTimeStamp,
        endExactTimeStamp: ExactTimeStamp?,
        scheduleTime: Time,
    ): Sequence<DateTime> {
        val startSoyDate = startExactTimeStamp.date.toDateSoy()
        var currentSoyDate = getNextValidDate(startSoyDate)

        val endSoyDate = endExactTimeStamp?.date?.toDateSoy()

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

            val tmpDateSoy = currentSoyDate
            currentSoyDate = getNextValidDate(currentSoyDate + 1.days)

            getDateTimeInDate(tmpDateSoy, startHourMilli, endHourMilli, scheduleTime) ?: Unit
        }.filterIsInstance<DateTime>()
    }

    private fun getDateTimeInDate(
        dateSoy: DateSoy,
        startHourMilli: HourMilli?,
        endHourMilli: HourMilli?,
        scheduleTime: Time,
    ): DateTime? {
        val hourMilli by lazy { scheduleTime.getHourMinute(DayOfWeek.fromDateSoy(dateSoy)).toHourMilli() }

        if (startHourMilli != null && startHourMilli > hourMilli) return null
        if (endHourMilli != null && endHourMilli <= hourMilli) return null

        return DateTime(Date(dateSoy), scheduleTime)
    }
}