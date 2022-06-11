package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ParentScheduleState
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class ExistingTaskEditDelegate(
    final override var data: EditViewModel.MainData,
    savedInstanceState: Bundle?,
    compositeDisposable: CompositeDisposable,
    storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : EditDelegate(savedInstanceState, compositeDisposable, storeParentKey) {

    protected val taskData get() = data.taskData!!

    override val initialName get() = taskData.name
    override val initialNote get() = taskData.note

    override val defaultScheduleStateProvider = DefaultScheduleStateProvider(null)

    override val defaultInitialParentScheduleState = taskData.run {
        ParentScheduleState.create(assignedTo, scheduleDataWrappers?.map(::ScheduleEntry)?.toList())
    }

    override fun checkImageChanged(editImageState: EditImageState): Boolean {
        val defaultEditImageState = taskData.imageState
            ?.let { EditImageState.Existing(it) }
            ?: EditImageState.None

        return editImageState != defaultEditImageState
    }

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}