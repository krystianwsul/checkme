package firebase.models.schedule.generators

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.soywiz.klock.months
import com.soywiz.klock.plus
import firebase.models.schedule.generators.DateTimeSequenceGenerator.Companion.toDate

abstract class MonthlyNewDateTimeSequenceGenerator : NewDateTimeSequenceGenerator() {

    protected abstract fun getDateInMonth(year: Int, month: Int): Date

    override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
        val startDate = startDateSoy.toDate()

        if (containsDate(startDate)) { // todo sequence optimize
            return startDateSoy
        } else {
            val dateSameMonth = getDateInMonth(startDate.year, startDate.month)

            val finalDate = when {
                dateSameMonth < startDate -> {
                    val nextMonthDateSoy = startDateSoy + 1.months
                    val nextMonthDate = nextMonthDateSoy.toDate()

                    getDateInMonth(nextMonthDate.year, nextMonthDate.month)
                }
                dateSameMonth > startDate -> dateSameMonth
                else -> throw IllegalStateException() // todo sequence redundant with first check
            }

            return finalDate.toDateSoy()
        }
    }
}