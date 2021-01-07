package com.krystianwsul.checkme.utils

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.rxjava3.core.Single
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
}!!