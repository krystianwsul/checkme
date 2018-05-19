package com.krystianwsul.checkme.utils

import android.content.Intent
import android.text.TextUtils
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import junit.framework.Assert
import java.util.*

object Utils {

    fun share(text: String) {
        Assert.assertTrue(!TextUtils.isEmpty(text))

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        MyApplication.instance.startActivity(Intent.createChooser(intent, MyApplication.instance.getString(R.string.sendTo)))
    }

    fun getDaysInMonth(year: Int, month: Int) = GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

    fun getDateInMonth(year: Int, month: Int, dayOfMonth: Int, beginningOfMonth: Boolean): Date {
        return if (beginningOfMonth) {
            Date(year, month, dayOfMonth)
        } else {
            Date(year, month, Utils.getDaysInMonth(year, month) - dayOfMonth + 1)
        }
    }

    fun getDateInMonth(year: Int, month: Int, dayOfMonth: Int, dayOfWeek: DayOfWeek, beginningOfMonth: Boolean): Date {
        if (beginningOfMonth) {
            val first = Date(year, month, 1)

            val day = if (dayOfWeek.ordinal >= first.dayOfWeek.ordinal) {
                (dayOfMonth - 1) * 7 + (dayOfWeek.ordinal - first.dayOfWeek.ordinal) + 1
            } else {
                dayOfMonth * 7 + (dayOfWeek.ordinal - first.dayOfWeek.ordinal) + 1
            }

            return Date(year, month, day)
        } else {
            val daysInMonth = getDaysInMonth(year, month)

            val last = Date(year, month, daysInMonth)

            val day = if (dayOfWeek.ordinal <= last.dayOfWeek.ordinal) {
                (dayOfMonth - 1) * 7 + (last.dayOfWeek.ordinal - dayOfWeek.ordinal) + 1
            } else {
                dayOfMonth * 7 + (last.dayOfWeek.ordinal - dayOfWeek.ordinal) + 1
            }

            return Date(year, month, daysInMonth - day + 1)
        }
    }

    fun ordinal(number: Int): String {
        var ret = number.toString()

        if (Locale.getDefault().language != "pl") {
            val mod = number % 10
            ret += when (mod) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }

        return ret
    }

    fun stringEquals(s1: String?, s2: String?) = if (TextUtils.isEmpty(s1)) {
        TextUtils.isEmpty(s2)
    } else {
        s1 == s2
    }
}
