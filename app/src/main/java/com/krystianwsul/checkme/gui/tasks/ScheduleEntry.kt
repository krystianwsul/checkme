package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.io.Serializable

abstract class ScheduleEntry(var error: String? = null) : Serializable {

    abstract val scheduleData: CreateTaskViewModel.ScheduleData

    abstract val scheduleType: ScheduleType

    abstract fun getText(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, context: Context): String

    abstract fun getScheduleDialogData(today: Date, scheduleHint: CreateTaskActivity.Hint.Schedule?): ScheduleDialogFragment.ScheduleDialogData
}
