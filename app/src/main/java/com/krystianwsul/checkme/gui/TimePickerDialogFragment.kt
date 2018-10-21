package com.krystianwsul.checkme.gui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import com.krystianwsul.checkme.utils.time.HourMinute


class TimePickerDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val HOUR_MINUTE_KEY = "hourMinute"

        fun newInstance(hourMinute: HourMinute) = TimePickerDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(HOUR_MINUTE_KEY, hourMinute)
            }
        }
    }

    lateinit var listener: (HourMinute) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) = arguments!!.run {
        check(containsKey(HOUR_MINUTE_KEY))

        val hourMinute = getParcelable<HourMinute>(HOUR_MINUTE_KEY)!!

        TimePickerDialog(
                activity,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute -> listener(HourMinute(hourOfDay, minute)) },
                hourMinute.hour,
                hourMinute.minute,
                DateFormat.is24HourFormat(activity))
    }
}
