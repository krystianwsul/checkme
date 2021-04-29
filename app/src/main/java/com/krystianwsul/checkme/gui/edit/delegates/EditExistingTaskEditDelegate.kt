package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.updateChildTask
import com.krystianwsul.checkme.domainmodel.extensions.updateScheduleTask
import com.krystianwsul.checkme.domainmodel.extensions.updateTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class EditExistingTaskEditDelegate(
        private val parameters: EditParameters.Edit,
        data: EditViewModel.MainData,
        savedInstanceState: Bundle?,
        compositeDisposable: CompositeDisposable,
        storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : ExistingTaskEditDelegate(data, savedInstanceState, compositeDisposable, storeParentKey) {

    override fun skipScheduleCheck(scheduleEntry: ScheduleEntry): Boolean {
        if (taskData.scheduleDataWrappers?.contains(scheduleEntry.scheduleDataWrapper) != true)
            return false

        val parentKey = parentScheduleManager.parent?.parentKey

        if (taskData.parentKey == parentKey)
            return true

        fun EditViewModel.ParentKey.getProjectId(): ProjectKey<*> = when (this) {
            is EditViewModel.ParentKey.Project -> projectId
            is EditViewModel.ParentKey.Task -> (taskKey as TaskKey.Project).projectKey // todo task edit2
        }

        val initialProject = taskData.parentKey?.getProjectId()

        val finalProject = parentKey?.getProjectId()

        return initialProject == finalProject
    }

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                parameters.taskKey,
                createParameters.name,
                scheduleDatas,
                createParameters.note,
                sharedProjectParameters,
                createParameters.editImageState.writeImagePath,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .toCreateResult()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.updateChildTask(
                DomainListenerManager.NotificationType.All,
                parameters.taskKey,
                createParameters.name,
                parentTaskKey,
                createParameters.note,
                createParameters.editImageState.writeImagePath,
                parameters.openedFromInstanceKey,
                createParameters.allReminders,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .toCreateResult()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.updateTopLevelTask(
                DomainListenerManager.NotificationType.All,
                parameters.taskKey,
                createParameters.name,
                createParameters.note,
                sharedProjectKey,
                createParameters.editImageState.writeImagePath,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .toCreateResult()
    }
}