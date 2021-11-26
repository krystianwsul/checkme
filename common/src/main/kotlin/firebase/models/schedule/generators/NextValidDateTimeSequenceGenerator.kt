package firebase.models.schedule.generators

import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.time.*
import com.soywiz.klock.days
import com.soywiz.klock.plus

abstract class NextValidDateTimeSequenceGenerator : DateTimeSequenceGenerator {

    private fun getNextValidDate(startDateSoy: DateSoy) = getNextValidDateHelper(startDateSoy).also {
        check(containsDateSoy(it)) // todo sequence checks
    }

    protected abstract fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy

    // todo sequence checks
    private fun getDateSequence(startDateSoy: DateSoy, endDateSoy: DateSoy?) =
        getDateSequenceHelper(startDateSoy, endDateSoy).also { it.onEach { check(containsDateSoy(it)) } }

    protected abstract fun getDateSequenceHelper(startDateSoy: DateSoy, endDateSoy: DateSoy?): Sequence<DateSoy>

    protected abstract fun containsDateSoy(dateSoy: DateSoy): Boolean // todo sequence checks

    /*
    todo sequence the algorithm here is kind of ridiculous.  getNextValidDate checks if the current date is valid for a given schedule
    (excluding start/end, from/until, etc.) and if not, returns the next date that is.  Then, we check that against the
    aforementioned.  And on the next loop, we increment the day by 1, then check it again.

    It would be more performant to directly generate a sequence of valid dates, with a given starting point.  And relatively
    straightforward once we have a valid date:

    Weekly: add a week timeSpan, recalculate
    Monthly*, Yearly: increment the month/year param, regenerate

    Furthermore, we should fetch the next possible date at the beginning of the next loop (with an edge case for the first
    iteration), instead of doing it at the end.  Unless we make a sequence of dates first, in which case looping won't
    be relevant.
     */
    override fun generate(
        startExactTimeStamp: ExactTimeStamp,
        endExactTimeStamp: ExactTimeStamp?,
        scheduleTime: Time,
    ): Sequence<DateTime> {
        val startSoyDate = startExactTimeStamp.dateSoy
        val endSoyDate = endExactTimeStamp?.dateSoy

        if (FeatureFlagManager.getFlag(FeatureFlagManager.Flag.SCHEDULE_DATE_SEQUENCE)) {
            return getDateSequence(startSoyDate, endSoyDate).mapNotNull { dateSoy ->
                val startHourMilli = startExactTimeStamp.takeIf { dateSoy == startSoyDate }?.hourMilli
                val endHourMilli = endExactTimeStamp?.takeIf { dateSoy == endSoyDate }?.hourMilli

                getDateTimeInDate(dateSoy, startHourMilli, endHourMilli, scheduleTime)
            }
        } else {
            var currentSoyDate = getNextValidDate(startSoyDate)

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