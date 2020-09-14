package com.krystianwsul.checkme.gui

import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

private fun Date.toMaterialDatePickerLong() = LocalDateTime(toMidnightExactTimeStamp().long).toDateTime(DateTimeZone.UTC).millis

fun Long.toMaterialDatePickerDate() = ExactTimeStamp(DateTime(this, DateTimeZone.UTC).millis).date

fun newMaterialDatePicker(date: Date, min: Date? = null): MaterialDatePicker<Long> {
    val startDate = min ?: Date.today()

    val dateLong = listOf(date, startDate).maxOrNull()!!.toMaterialDatePickerLong()
    val startDateLong = startDate.toMaterialDatePickerLong()

    return MaterialDatePicker.Builder
            .datePicker()
            .setSelection(dateLong)
            .setCalendarConstraints(
                    CalendarConstraints.Builder()
                            .setValidator(DateValidatorPointForward.from(startDateLong))
                            .setStart(startDateLong)
                            .setOpenAt(dateLong)
                            .build()
            )
            .build()
}

fun newYearMaterialDatePicker(date: Date): MaterialDatePicker<Long> {
    val min = Date(date.year, 1, 1).toMaterialDatePickerLong()
    val max = Date(date.year, 12, 31).toMaterialDatePickerLong()

    val dateLong = date.toMaterialDatePickerLong()

    return MaterialDatePicker.Builder
            .datePicker()
            .setSelection(dateLong)
            .setCalendarConstraints(
                    CalendarConstraints.Builder()
                            .setStart(min)
                            .setEnd(max)
                            .setOpenAt(dateLong)
                            .build()
            )
            .build()
}

fun MaterialDatePicker<Long>.addListener(listener: (Date) -> Unit) = addOnPositiveButtonClickListener {
    listener(it.toMaterialDatePickerDate())
}

@Suppress("UNCHECKED_CAST")
fun FragmentManager.getMaterialDatePicker(key: String) = findFragmentByTag(key) as? MaterialDatePicker<Long>