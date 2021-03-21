package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.ParentMultiScheduleManager
import com.krystianwsul.checkme.gui.edit.ParentScheduleState
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.checkme.viewmodels.EditViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class ExistingTaskEditDelegate(
        final override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        private val editImageState: EditImageState?,
        compositeDisposable: CompositeDisposable,
) : EditDelegate(editImageState, compositeDisposable) {

    protected val taskData get() = data.taskData!!

    override val initialName get() = taskData.name
    override val initialNote get() = taskData.note

    override val parentScheduleManager = ParentMultiScheduleManager(
            savedInstanceState,
            {
                ParentScheduleState.create(
                        taskData.parentKey,
                        taskData.assignedTo,
                        taskData.scheduleDataWrappers
                                ?.map { ScheduleEntry(it) }
                                ?.toList(),
                )
            },
            parentLookup,
    )

    override fun getInitialEditImageState(): EditImageState {
        return when {
            editImageState?.dontOverwrite == true -> editImageState
            taskData.imageState != null -> EditImageState.Existing(taskData.imageState!!)
            editImageState != null -> editImageState
            else -> EditImageState.None
        }
    }

    override fun checkImageChanged(): Boolean {
        val defaultEditImageState = taskData.imageState
                ?.let { EditImageState.Existing(it) }
                ?: EditImageState.None

        return imageUrl.value != defaultEditImageState
    }

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}