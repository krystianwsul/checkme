package com.krystianwsul.checkme.utils

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.viewmodels.NullableWrapper
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

    fun getDaysInMonth(year: Int, month: Int) = GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)

    fun getDateInMonth(year: Int, month: Int, dayOfMonth: Int, beginningOfMonth: Boolean): Date {
        return if (beginningOfMonth) {
            Date(year, month, dayOfMonth)
        } else {
            Date(year, month, getDaysInMonth(year, month) - dayOfMonth + 1)
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
}

inline fun <reified T, U> T.getPrivateField(name: String): U {
    return T::class.java.getDeclaredField(name).let {
        it.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        it.get(this) as U
    }
}

fun <T> Task<T>.toSingle() = Single.create<NullableWrapper<T>> { subscriber ->
    addOnCompleteListener {
        subscriber.onSuccess(NullableWrapper(it.takeIf { it.isSuccessful }?.result))
    }
}