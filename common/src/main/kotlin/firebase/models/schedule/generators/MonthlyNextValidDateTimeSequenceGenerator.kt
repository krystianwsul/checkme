package firebase.models.schedule.generators

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.soywiz.klock.months
import com.soywiz.klock.plus
import firebase.models.schedule.generators.DateTimeSequenceGenerator.Companion.toDate

abstract class MonthlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

    protected abstract fun getDateInMonth(year: Int, month: Int): Date // todo sequence toDate

    override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
        val startDate = startDateSoy.toDate()

        val dateSameMonth = getDateInMonth(startDate.year, startDate.month)

        return when (val comparison = dateSameMonth.compareTo(startDate)) {
            -1 -> {
                val nextMonthDateSoy = startDateSoy + 1.months

                getDateInMonth(nextMonthDateSoy.year, nextMonthDateSoy.month1).toDateSoy()
            }
            1 -> dateSameMonth.toDateSoy()
            else -> {
                check(comparison == 0) // todo sequence checks

                startDateSoy
            }
        }
    }
}