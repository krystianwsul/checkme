package com.krystianwsul.checkme.gui.tasks.create.delegates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskImageState
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskParameters
import com.krystianwsul.checkme.gui.tasks.create.ParentScheduleState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CopyCreateTaskDelegate(
        private val parameters: CreateTaskParameters.Copy,
        data: CreateTaskViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, CreateTaskImageState>?
) : ExistingCreateTaskDelegate(data, savedStates) {

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
                .also { CreateTaskActivity.createdTaskKey = it }
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
                .also { CreateTaskActivity.createdTaskKey = it }
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
                .also { CreateTaskActivity.createdTaskKey = it }
    }
}