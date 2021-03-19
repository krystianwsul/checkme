package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.updateChildTask
import com.krystianwsul.checkme.domainmodel.extensions.updateRootTask
import com.krystianwsul.checkme.domainmodel.extensions.updateScheduleTask
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class EditExistingTaskEditDelegate(
        private val parameters: EditParameters.Edit,
        data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?,
        compositeDisposable: CompositeDisposable,
) : ExistingTaskEditDelegate(data, savedInstanceState, editImageState, compositeDisposable) {

    override fun skipScheduleCheck(scheduleEntry: ScheduleEntry): Boolean {
        if (taskData.scheduleDataWrappers?.contains(scheduleEntry.scheduleDataWrapper) != true)
            return false

        val parentKey = parentScheduleManager.parent?.parentKey

        if (taskData.parentKey == parentKey)
            return true

        fun EditViewModel.ParentKey.getProjectId() = when (this) {
            is EditViewModel.ParentKey.Project -> projectId
            is EditViewModel.ParentKey.Task -> parentLookup.findTaskData(this).projectId
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

        return DomainFactory.instance
                .updateScheduleTask(
                        DomainListenerManager.NotificationType.All,
                        SaveService.Source.GUI,
                        parameters.taskKey,
                        createParameters.name,
                        scheduleDatas,
                        createParameters.note,
                        sharedProjectParameters,
                        imageUrl.value!!.writeImagePath,
                )
                .toCreateResult()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        return DomainFactory.instance
                .updateChildTask(
                        DomainListenerManager.NotificationType.All,
                        SaveService.Source.GUI,
                        parameters.taskKey,
                        createParameters.name,
                        parentTaskKey,
                        createParameters.note,
                        imageUrl.value!!.writeImagePath,
                        parameters.openedFromInstanceKey,
                        createParameters.allReminders,
                )
                .toCreateResult()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .updateRootTask(
                        DomainListenerManager.NotificationType.All,
                        SaveService.Source.GUI,
                        parameters.taskKey,
                        createParameters.name,
                        createParameters.note,
                        sharedProjectKey,
                        imageUrl.value!!.writeImagePath,
                )
                .toCreateResult()
    }
}