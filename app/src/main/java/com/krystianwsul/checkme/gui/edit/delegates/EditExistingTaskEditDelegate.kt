package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class EditExistingTaskEditDelegate(
        private val parameters: EditParameters.Edit,
        data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?
) : ExistingTaskEditDelegate(data, savedInstanceState, editImageState) {

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
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance.updateScheduleTask(
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                scheduleDatas,
                createParameters.note,
                projectKey,
                imageUrl.value!!.writeImagePath
        )
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance.updateChildTask(
                ExactTimeStamp.now,
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                parentTaskKey,
                createParameters.note,
                imageUrl.value!!.writeImagePath
        )
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance.updateRootTask(
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                createParameters.note,
                projectKey,
                imageUrl.value!!.writeImagePath
        )
    }
}