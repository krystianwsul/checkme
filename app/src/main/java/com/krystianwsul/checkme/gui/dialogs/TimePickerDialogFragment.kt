package com.krystianwsul.checkme.gui.dialogs

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.common.time.HourMinute
import java.io.Serializable


class TimePickerDialogFragment<T> : AbstractDialogFragment() {

    companion object {

        private const val HOUR_MINUTE_KEY = "hourMinute"
        private const val KEY_EXTRA = "extra"

        fun <T : Serializable> newInstance(hourMinute: HourMinute, extra: T) = TimePickerDialogFragment<T>().apply {
            arguments = Bundle().apply {
                putParcelable(HOUR_MINUTE_KEY, hourMinute)
                putSerializable(KEY_EXTRA, extra)
            }
        }
    }

    lateinit var listener: (HourMinute, T) -> Unit

    @Suppress("UNCHECKED_CAST")
    override fun onCreateDialog(savedInstanceState: Bundle?) = requireArguments().run {
        check(containsKey(HOUR_MINUTE_KEY))

        val hourMinute = getParcelable<HourMinute>(HOUR_MINUTE_KEY)!!
        val extra = getSerializable(KEY_EXTRA) as T

        TimePickerDialog(
                requireActivity(),
                { _, hourOfDay, minute -> listener(HourMinute(hourOfDay, minute), extra) },
                hourMinute.hour,
                hourMinute.minute,
                DateFormat.is24HourFormat(requireActivity())
        )
    }
}
