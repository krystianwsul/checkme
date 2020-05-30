package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CopyExistingTaskEditDelegate(
        private val parameters: EditParameters.Copy,
        data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?
) : ExistingTaskEditDelegate(data, savedInstanceState, editImageState) {

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        check(createParameters.allReminders)

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
        check(createParameters.allReminders)

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
        check(createParameters.allReminders)

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