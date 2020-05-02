package com.krystianwsul.checkme.gui

import android.app.DatePickerDialog
import android.os.Bundle
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimeStamp


class DatePickerDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val DATE_KEY = "date"
        private const val KEY_MIN = "min"
        private const val KEY_YEAR = "year"

        fun newInstance(date: Date, min: Date? = null) = DatePickerDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(DATE_KEY, date)
                putParcelable(KEY_MIN, min)
            }
        }

        fun newYearInstance(date: Date) = DatePickerDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(DATE_KEY, date)
                putBoolean(KEY_YEAR, true)
            }
        }
    }

    lateinit var listener: (Date) -> Unit

    private val onDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        listener(Date(year, month + 1, dayOfMonth))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = requireArguments().getParcelable<Date>(DATE_KEY)!!.let { date ->
        DatePickerDialog(
                requireActivity(),
                onDateSetListener,
                date.year,
                date.month - 1,
                date.day
        ).apply {
            if (requireArguments().getBoolean(KEY_YEAR)) {
                datePicker.minDate = TimeStamp(Date(date.year, 1, 1), HourMinute.now).long
                datePicker.maxDate = TimeStamp(Date(date.year, 12, 31), HourMinute.now).long
            } else {
                datePicker.minDate = requireArguments().getParcelable<Date>(KEY_MIN)
                        ?.let { TimeStamp(it, HourMinute.now).long }
                        ?: ExactTimeStamp.now.long
            }
        }
    }
}
