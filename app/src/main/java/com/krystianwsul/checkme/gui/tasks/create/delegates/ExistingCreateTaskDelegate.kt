package com.krystianwsul.checkme.gui.tasks.create.delegates

import com.krystianwsul.checkme.gui.tasks.ScheduleEntry
import com.krystianwsul.checkme.gui.tasks.create.ParentScheduleState
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel

abstract class ExistingCreateTaskDelegate(
        override final var data: CreateTaskViewModel.Data,
        savedStates: Pair<ParentScheduleState, ParentScheduleState>?
) : CreateTaskDelegate() {

    protected val taskData get() = data.taskData!!

    override val initialName get() = taskData.name

    override val initialState = savedStates?.first ?: ParentScheduleState.create(
            taskData.parentKey,
            taskData.scheduleDataWrappers
                    ?.map { ScheduleEntry(it) }
                    ?.toList()
    )

    override val parentScheduleManager = getParentScheduleManager(savedStates?.second)

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}