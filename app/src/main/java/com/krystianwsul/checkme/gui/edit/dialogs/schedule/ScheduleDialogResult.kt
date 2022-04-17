package com.krystianwsul.checkme.gui.edit.dialogs.schedule

sealed class ScheduleDialogResult {

    class Change(
        val position: Int?,
        val scheduleDialogData: ScheduleDialogData
    ) : ScheduleDialogResult()

    class Delete(val position: Int) : ScheduleDialogResult()

    object Cancel : ScheduleDialogResult()

    class Copy(val scheduleDialogData: ScheduleDialogData) : ScheduleDialogResult()
}