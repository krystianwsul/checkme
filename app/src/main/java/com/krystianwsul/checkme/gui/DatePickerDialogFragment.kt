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

        fun newInstance(date: Date, min: Date? = null) = DatePickerDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(DATE_KEY, date)
                putParcelable(KEY_MIN, min)
            }
        }
    }

    lateinit var listener: (Date) -> Unit

    private val mOnDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        listener(Date(year, month + 1, dayOfMonth))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = arguments!!.getParcelable<Date>(DATE_KEY)!!.let { date ->
        DatePickerDialog(
                requireActivity(),
                mOnDateSetListener,
                date.year,
                date.month - 1,
                date.day
        ).apply {
            datePicker.minDate = arguments!!.getParcelable<Date>(KEY_MIN)
                    ?.let { TimeStamp(it, HourMinute.now).long }
                    ?: ExactTimeStamp.now.long
        }
    }
}
