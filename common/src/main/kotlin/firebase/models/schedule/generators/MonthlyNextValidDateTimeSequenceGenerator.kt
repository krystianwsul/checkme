package firebase.models.schedule.generators

import com.krystianwsul.common.time.DateSoy
import com.soywiz.klock.months
import com.soywiz.klock.plus

abstract class MonthlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

    protected abstract fun getDateSoyInMonth(year: Int, month: Int): DateSoy

    override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
        val dateSoySameMonth = getDateSoyInMonth(startDateSoy.year, startDateSoy.month1)

        return when (val comparison = dateSoySameMonth.compareTo(startDateSoy)) {
            -1 -> {
                val nextMonthDateSoy = startDateSoy + 1.months

                getDateSoyInMonth(nextMonthDateSoy.year, nextMonthDateSoy.month1)
            }
            1 -> dateSoySameMonth
            else -> {
                check(comparison == 0) // todo sequence checks

                startDateSoy
            }
        }
    }

    override fun getDateSequenceHelper(startDateSoy: DateSoy, endDateSoy: DateSoy?): Sequence<DateSoy> {
        var currentDateSoy = getNextValidDateHelper(startDateSoy)

        return generateSequence {
            val tmpDateSoy = currentDateSoy

            currentDateSoy += 1.months

            getDateSoyInMonth(tmpDateSoy.year, tmpDateSoy.month1).takeIf {
                if (endDateSoy != null)
                    it <= endDateSoy
                else
                    true
            }
        }
    }
}