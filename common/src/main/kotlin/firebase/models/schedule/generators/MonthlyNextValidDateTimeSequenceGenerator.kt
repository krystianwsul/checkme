package firebase.models.schedule.generators

import com.krystianwsul.common.time.DateSoy
import com.soywiz.klock.months
import com.soywiz.klock.plus

abstract class MonthlyNextValidDateTimeSequenceGenerator : NextValidDateTimeSequenceGenerator() {

    protected abstract fun getDateSoyInMonth(year: Int, month: Int): DateSoy

    override fun getFirstDateSoy(startDateSoy: DateSoy): DateSoy {
        val dateSoySameMonth = getDateSoyInMonth(startDateSoy.year, startDateSoy.month1)

        return when (dateSoySameMonth.compareTo(startDateSoy)) {
            -1 -> {
                val nextMonthDateSoy = startDateSoy + 1.months

                getDateSoyInMonth(nextMonthDateSoy.year, nextMonthDateSoy.month1)
            }
            1 -> dateSoySameMonth
            else -> startDateSoy
        }
    }

    override fun getNextDateSoy(currentDateSoy: DateSoy) =
        (currentDateSoy + 1.months).run { getDateSoyInMonth(year, month1) }
}