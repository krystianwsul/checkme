package firebase.models.schedule.generators

import com.krystianwsul.common.time.*

abstract class NextValidDateTimeSequenceGenerator : DateTimeSequenceGenerator {

    companion object {

        fun DateSoy.filterEnd(endDateSoy: DateSoy?): DateSoy? {
            if (endDateSoy == null) return this

            return takeIf { endDateSoy >= it }
        }
    }

    private fun getDateSequence(startDateSoy: DateSoy, endDateSoy: DateSoy?): Sequence<DateSoy> {
        return generateSequence(
            { getFirstDateSoy(startDateSoy).filterEnd(endDateSoy) },
            { getNextDateSoy(it).filterEnd(endDateSoy) },
        ).onEach { check(containsDateSoy(it)) } // todo sequence checks
    }

    protected abstract fun getFirstDateSoy(startDateSoy: DateSoy): DateSoy

    protected abstract fun getNextDateSoy(currentDateSoy: DateSoy): DateSoy

    protected abstract fun containsDateSoy(dateSoy: DateSoy): Boolean // todo sequence checks

    override fun generate(
        startExactTimeStamp: ExactTimeStamp,
        endExactTimeStamp: ExactTimeStamp?,
        scheduleTime: Time,
    ): Sequence<DateTime> {
        val startSoyDate = startExactTimeStamp.dateSoy
        val endSoyDate = endExactTimeStamp?.dateSoy

        return getDateSequence(startSoyDate, endSoyDate).mapNotNull { dateSoy ->
            val startHourMilli = startExactTimeStamp.takeIf { dateSoy == startSoyDate }?.hourMilli
            val endHourMilli = endExactTimeStamp?.takeIf { dateSoy == endSoyDate }?.hourMilli

            getDateTimeInDate(dateSoy, startHourMilli, endHourMilli, scheduleTime)
        }
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