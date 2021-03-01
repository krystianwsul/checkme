package com.krystianwsul.checkme.gui.dialogs

import android.content.Context
import android.text.format.DateFormat
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.krystianwsul.common.time.HourMinute
import java.io.Serializable

private const val KEY_EXTRA = "extra"

private fun getTimeFormat(context: Context) =
        if (DateFormat.is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

fun newMaterialTimePicker(
        context: Context,
        fragmentManager: FragmentManager,
        tag: String,
        hourMinute: HourMinute,
        extra: Serializable? = null,
) = MaterialTimePicker.Builder()
        .setTimeFormat(getTimeFormat(context))
        .setHour(hourMinute.hour)
        .setMinute(hourMinute.minute)
        .build()
        .apply {
            extra?.let { requireArguments().putSerializable(KEY_EXTRA, it) }

            show(fragmentManager, tag)
        }

fun MaterialTimePicker.setListener(listener: (HourMinute) -> Unit) {
    addOnPositiveButtonClickListener { listener(HourMinute(hour, minute)) }
}

fun <T : Serializable> MaterialTimePicker.setListener(listener: (HourMinute, T) -> Unit) {
    addOnPositiveButtonClickListener {
        @Suppress("UNCHECKED_CAST")
        listener(HourMinute(hour, minute), requireArguments().getSerializable(KEY_EXTRA) as T)
    }
}