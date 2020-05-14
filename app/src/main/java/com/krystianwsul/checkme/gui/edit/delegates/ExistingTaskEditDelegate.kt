package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.ParentMultiScheduleManager
import com.krystianwsul.checkme.gui.edit.ParentScheduleState
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.checkme.viewmodels.EditViewModel

abstract class ExistingTaskEditDelegate(
        final override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?
) : EditDelegate(editImageState) {

    protected val taskData get() = data.taskData!!

    override val initialName get() = taskData.name
    override val showSaveAndOpen = false

    override val parentScheduleManager = ParentMultiScheduleManager(
            savedInstanceState,
            {
                ParentScheduleState.create(
                        taskData.parentKey,
                        taskData.scheduleDataWrappers
                                ?.map { ScheduleEntry(it) }
                                ?.toList()
                )
            },
            parentLookup
    )

    final override val imageUrl: BehaviorRelay<EditImageState>

    init {
        val final = when {
            editImageState?.dontOverwrite == true -> editImageState
            taskData.imageState != null -> EditImageState.Existing(taskData.imageState!!)
            editImageState != null -> editImageState
            else -> EditImageState.None
        }

        imageUrl = BehaviorRelay.createDefault(final)
    }

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}