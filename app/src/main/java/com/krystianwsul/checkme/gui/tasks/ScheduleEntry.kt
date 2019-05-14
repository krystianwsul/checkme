package com.krystianwsul.checkme.gui.tasks

import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import java.io.Serializable

class ScheduleEntry(val scheduleData: CreateTaskViewModel.ScheduleData, var error: String? = null) : Serializable
