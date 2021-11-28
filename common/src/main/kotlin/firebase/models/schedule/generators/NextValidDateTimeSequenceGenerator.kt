package firebase.models.schedule.generators

import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.time.*
import com.soywiz.klock.days
import com.soywiz.klock.plus

abstract class NextValidDateTimeSequenceGenerator : DateTimeSequenceGenerator {

    companion object {

        fun DateSoy.filterEnd(endDateSoy: DateSoy?): DateSoy? {
            if (endDateSoy == null) return this

            return takeIf { endDateSoy >= it }
        }
    }

    private fun getFirstDateSoy(startDateSoy: DateSoy) = getFirstDateSoyHelper(startDateSoy).also {
        check(containsDateSoy(it)) // todo sequence checks
    }

    private fun getDateSequence(startDateSoy: DateSoy, endDateSoy: DateSoy?): Sequence<DateSoy> {
        return generateSequence(
            { getFirstDateSoyHelper(startDateSoy).filterEnd(endDateSoy) },
            { getNextDateSoy(it).filterEnd(endDateSoy) },
        ).onEach { check(containsDateSoy(it)) } // todo sequence checks
    }

    protected abstract fun getFirstDateSoyHelper(startDateSoy: DateSoy): DateSoy

    protected abstract fun getNextDateSoy(currentDateSoy: DateSoy): DateSoy

    protected abstract fun containsDateSoy(dateSoy: DateSoy): Boolean // todo sequence checks

    override fun generate(
        startExactTimeStamp: ExactTimeStamp,
        endExactTimeStamp: ExactTimeStamp?,
        scheduleTime: Time,
    ): Sequence<DateTime> {
        val startSoyDate = startExactTimeStamp.dateSoy
        val endSoyDate = endExactTimeStamp?.dateSoy

        if (FeatureFlagManager.getFlag(FeatureFlagManager.Flag.SCHEDULE_DATE_SEQUENCE) || true) {
            return getDateSequence(startSoyDate, endSoyDate).mapNotNull { dateSoy ->
                val startHourMilli = startExactTimeStamp.takeIf { dateSoy == startSoyDate }?.hourMilli
                val endHourMilli = endExactTimeStamp?.takeIf { dateSoy == endSoyDate }?.hourMilli

                getDateTimeInDate(dateSoy, startHourMilli, endHourMilli, scheduleTime)
            }
        } else {
            var currentSoyDate = getFirstDateSoy(startSoyDate)

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
                currentSoyDate = getFirstDateSoy(currentSoyDate + 1.days)

                getDateTimeInDate(tmpDateSoy, startHourMilli, endHourMilli, scheduleTime) ?: Unit
            }.filterIsInstance<DateTime>()
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