package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.updateChildTask
import com.krystianwsul.checkme.domainmodel.extensions.updateRootTask
import com.krystianwsul.checkme.domainmodel.extensions.updateScheduleTask
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.time.ExactTimeStamp
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

    override fun showAllRemindersDialog(): Boolean? {
        if (!data.showAllInstancesDialog)
            return null

        val parent = parentScheduleManager.parent
        if (parent?.isRootTaskGroup != true)
            return null

        return false
    }

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance.updateScheduleTask(
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                scheduleDatas,
                createParameters.note,
                sharedProjectParameters,
                imageUrl.value!!.writeImagePath
        )
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        return DomainFactory.instance.updateChildTask(
                ExactTimeStamp.Local.now,
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                parentTaskKey,
                createParameters.note,
                imageUrl.value!!.writeImagePath,
                parameters.removeInstanceKey,
                createParameters.allReminders
        )
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectParameters: SharedProjectParameters?,
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance.updateRootTask(
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                createParameters.note,
                sharedProjectParameters,
                imageUrl.value!!.writeImagePath
        )
    }
}