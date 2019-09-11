package com.krystianwsul.checkme.utils

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.soywiz.klock.Month
import io.reactivex.Single
import java.util.*

object Utils {

    fun share(activity: Activity, text: String) {
        check(!TextUtils.isEmpty(text))

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.sendTo)))
    }

    fun getDateInMonth(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            beginningOfMonth: Boolean
    ) = Date(year, month, if (beginningOfMonth) dayOfMonth else Month(month).days(year) - dayOfMonth + 1)

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
            val daysInMonth = Month(month).days(year)

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
}

fun <T> Task<T>.toSingle() = Single.create<NullableWrapper<T>> { subscriber ->
    addOnCompleteListener {
        subscriber.onSuccess(NullableWrapper(it.takeIf { it.isSuccessful }?.result))
    }
}