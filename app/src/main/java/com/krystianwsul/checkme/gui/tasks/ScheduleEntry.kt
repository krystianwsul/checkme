package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.os.Parcelable
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel

abstract class ScheduleEntry(var error: String?) : Parcelable {

    companion object {

        fun fromScheduleDialogData(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData): ScheduleEntry {
            return when (scheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> SingleScheduleEntry(scheduleDialogData)
                ScheduleType.WEEKLY -> WeeklyScheduleEntry(scheduleDialogData)
                ScheduleType.MONTHLY_DAY -> MonthlyDayScheduleEntry(scheduleDialogData)
                ScheduleType.MONTHLY_WEEK -> MonthlyWeekScheduleEntry(scheduleDialogData)
                else -> throw UnsupportedOperationException()
            }
        }
    }

    abstract val scheduleData: CreateTaskViewModel.ScheduleData

    abstract val scheduleType: ScheduleType

    constructor() : this(null)

    abstract fun getText(customTimeDatas: Map<CustomTimeKey, CreateTaskViewModel.CustomTimeData>, context: Context): String

    abstract fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.ScheduleHint?): ScheduleDialogFragment.ScheduleDialogData
}
