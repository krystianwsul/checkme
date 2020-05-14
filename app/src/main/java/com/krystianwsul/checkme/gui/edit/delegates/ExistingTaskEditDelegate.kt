package com.krystianwsul.checkme.gui.edit.delegates

import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.ParentMultiScheduleManager
import com.krystianwsul.checkme.gui.edit.ParentScheduleState
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.checkme.viewmodels.EditViewModel

abstract class ExistingTaskEditDelegate(
        final override var data: EditViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, EditImageState>?
) : EditDelegate(savedStates?.third) {

    protected val taskData get() = data.taskData!!

    override val initialName get() = taskData.name
    override val showSaveAndOpen = false

    final override val initialState = savedStates?.first ?: ParentScheduleState.create(
            taskData.parentKey,
            taskData.scheduleDataWrappers
                    ?.map { ScheduleEntry(it) }
                    ?.toList()
    )

    override val parentScheduleManager = ParentMultiScheduleManager(savedStates?.second, initialState, parentLookup)

    final override val imageUrl: BehaviorRelay<EditImageState>

    init {
        val final = when {
            savedStates?.third?.dontOverwrite == true -> savedStates.third
            taskData.imageState != null -> EditImageState.Existing(taskData.imageState!!)
            savedStates?.third != null -> savedStates.third
            else -> EditImageState.None
        }

        imageUrl = BehaviorRelay.createDefault(final)
    }

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}