package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.gui.edit.*
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class ExistingTaskEditDelegate(
        final override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        compositeDisposable: CompositeDisposable,
        storeParent: (EditViewModel.ParentTreeData?) -> Unit,
) : EditDelegate(compositeDisposable, storeParent) {

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
            callbacks,
    )

    override fun checkImageChanged(editImageState: EditImageState): Boolean {
        val defaultEditImageState = taskData.imageState
                ?.let { EditImageState.Existing(it) }
                ?: EditImageState.None

        return editImageState != defaultEditImageState
    }

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)
}