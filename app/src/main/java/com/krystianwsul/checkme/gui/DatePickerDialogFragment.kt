package com.krystianwsul.checkme.gui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp


class DatePickerDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val DATE_KEY = "date"

        fun newInstance(date: Date) = DatePickerDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(DATE_KEY, date)
            }
        }
    }

    lateinit var listener: (Date) -> Unit

    private val mOnDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        listener(Date(year, month + 1, dayOfMonth))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(arguments!!.containsKey(DATE_KEY))

        val date = arguments!!.getParcelable<Date>(DATE_KEY)!!

        val datePickerDialog = DatePickerDialog(requireActivity(), mOnDateSetListener, date.year, date.month - 1, date.day)
        datePickerDialog.datePicker.minDate = ExactTimeStamp.now.long - 1000 // -1000 odejmuje sekundę żeby obejść bug na ver. < 5.0

        return datePickerDialog
    }
}
