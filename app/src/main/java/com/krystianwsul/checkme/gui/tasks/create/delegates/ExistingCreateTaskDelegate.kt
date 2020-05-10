package com.krystianwsul.checkme.gui.tasks.create.delegates

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.gui.tasks.ScheduleEntry
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskImageState
import com.krystianwsul.checkme.gui.tasks.create.ParentScheduleState
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel

abstract class ExistingCreateTaskDelegate(
        final override var data: CreateTaskViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, CreateTaskImageState>?
) : CreateTaskDelegate(savedStates?.third) {

    protected val taskData get() = data.taskData!!

    override val initialName get() = taskData.name
    override val showSaveAndOpen = false

    override val initialState = savedStates?.first ?: ParentScheduleState.create(
            taskData.parentKey,
            taskData.scheduleDataWrappers
                    ?.map { ScheduleEntry(it) }
                    ?.toList()
    )

    override val parentScheduleManager = getParentScheduleManager(savedStates?.second)

    final override val imageUrl: BehaviorRelay<CreateTaskImageState>

    init {
        val final = when {
            savedStates?.third?.dontOverwrite == true -> savedStates.third
            taskData.imageState != null -> CreateTaskImageState.Existing(taskData.imageState!!)
            savedStates?.third != null -> savedStates.third
            else -> CreateTaskImageState.None
        }

        imageUrl = BehaviorRelay.createDefault(final)
    }

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}