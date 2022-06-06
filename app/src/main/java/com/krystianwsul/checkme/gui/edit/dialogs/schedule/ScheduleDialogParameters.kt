package com.krystianwsul.checkme.gui.edit.dialogs.schedule

import com.krystianwsul.common.utils.TaskKey

class ScheduleDialogParameters(
    val scheduleDialogData: ScheduleDialogData,
    val excludedTaskKeys: Set<TaskKey>,
    val position: Int? = null,
)