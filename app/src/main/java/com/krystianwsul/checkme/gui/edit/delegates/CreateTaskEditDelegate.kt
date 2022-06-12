package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.utils.exhaustive
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CreateTaskEditDelegate(
    private val parameters: EditParameters.CreateDelegateParameters,
    override var data: EditViewModel.MainData,
    savedInstanceState: Bundle?,
    compositeDisposable: CompositeDisposable,
    storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : EditDelegate(parameters, savedInstanceState, compositeDisposable, storeParentKey) {

    override val initialName: String? // todo cleanup
    override val showSaveAndOpen = true

    init {
        when (parameters) {
            is EditParameters.Create -> {
                initialName = parameters.nameHint
            }
            is EditParameters.MigrateDescription -> {
                initialName = data.parentTaskDescription!!
            }
            is EditParameters.Share -> {
                initialName = parameters.nameHint
            }
            is EditParameters.Shortcut -> {
                initialName = null
            }
            EditParameters.None -> {
                initialName = null
            }
        }.exhaustive()
    }

    override fun showDialog(): ShowDialog {
        val parent = parentScheduleManager.parent ?: return ShowDialog.NONE

        val parentTaskKey = parent.parentKey
            .let { it as? EditViewModel.ParentKey.Task }
            ?.taskKey
            ?: return ShowDialog.NONE

        if (parentTaskKey != (parameters as? EditParameters.Create)?.hint?.instanceKey?.taskKey) {
            check(parent.hasMultipleInstances == null)

            return ShowDialog.NONE
        }

        return if (parent.hasMultipleInstances!!) ShowDialog.ADD else ShowDialog.NONE
    }

    override fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        projectParameters: ProjectParameters?,
        joinAllInstances: Boolean?,
    ): Single<CreateResult> {
        check(joinAllInstances == null)

        return AndroidDomainUpdater.createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            scheduleDatas,
            projectParameters,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        dialogResult: DialogResult,
    ): Single<CreateResult> {
        if (parameters is EditParameters.Share) ShortcutManager.addShortcut(parentTaskKey)

        val parentParameter = if (dialogResult.addToAllInstances == false) {
            val parentInstanceKey = (parameters as EditParameters.Create).hint!!.instanceKey!!
            check(parentInstanceKey.taskKey == parentTaskKey)

            CreateChildTaskDomainUpdate.Parent.Instance(parentInstanceKey)
        } else {
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey)
        }

        return CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            parentParameter,
            createParameters,
            clearParentNote = parentTaskKey == (parameters as? EditParameters.MigrateDescription)?.taskKey,
        )
            .perform(AndroidDomainUpdater)
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
        createParameters: CreateParameters,
        projectKey: ProjectKey<*>?,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.createTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            projectKey,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }
}