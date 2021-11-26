package firebase.models.schedule.generators

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.soywiz.klock.months
import com.soywiz.klock.plus
import firebase.models.schedule.generators.DateTimeSequenceGenerator.Companion.toDate

abstract class MonthlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

    protected abstract fun getDateInMonth(year: Int, month: Int): Date // todo sequence optimize consider soy signature

    override fun getNextValidDateHelper(startDateSoy: DateSoy): DateSoy {
        val startDate = startDateSoy.toDate()

        val dateSameMonth = getDateInMonth(startDate.year, startDate.month)

        return when { // todo sequence optimize execute comparison once
            dateSameMonth < startDate -> {
                val nextMonthDateSoy = startDateSoy + 1.months
                val nextMonthDate = nextMonthDateSoy.toDate()

                getDateInMonth(nextMonthDate.year, nextMonthDate.month).toDateSoy()
            }
            dateSameMonth > startDate -> dateSameMonth.toDateSoy()
            else -> startDateSoy
        }
    }
}