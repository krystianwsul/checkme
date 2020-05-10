package com.krystianwsul.checkme.gui.edit.delegates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.ParentScheduleState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CopyExistingTaskEditDelegate(
        private val parameters: EditParameters.Copy,
        data: EditViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, EditImageState>?
) : ExistingTaskEditDelegate(data, savedStates) {

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        return DomainFactory.instance
                .createScheduleRootTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        createParameters.name,
                        scheduleDatas,
                        createParameters.note,
                        projectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.taskKey
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        return DomainFactory.instance
                .createChildTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        createParameters.note,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.taskKey
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        return DomainFactory.instance
                .createRootTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        createParameters.name,
                        createParameters.note,
                        projectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.taskKey
                )
                .also { EditActivity.createdTaskKey = it }
    }
}