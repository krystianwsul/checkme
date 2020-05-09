package com.krystianwsul.checkme.gui.tasks.create.delegates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.ScheduleEntry
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskParameters
import com.krystianwsul.checkme.gui.tasks.create.ParentScheduleState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class EditCreateTaskDelegate(
        private val parameters: CreateTaskParameters.Edit,
        override var data: CreateTaskViewModel.Data,
        savedStates: Pair<ParentScheduleState, ParentScheduleState>?
) : CreateTaskDelegate() {

    private val taskData get() = data.taskData!!

    override val initialName get() = taskData.name

    override val initialState = savedStates?.first ?: ParentScheduleState.create(
            taskData.parentKey,
            taskData.scheduleDataWrappers
                    ?.map { ScheduleEntry(it) }
                    ?.toList()
    )

    override val parentScheduleManager = getParentScheduleManager(savedStates?.second)

    override fun checkNameNoteChanged(name: String, note: String?) = checkNameNoteChanged(taskData, name, note)

    override fun skipScheduleCheck(scheduleEntry: ScheduleEntry): Boolean {
        if (taskData.scheduleDataWrappers?.contains(scheduleEntry.scheduleDataWrapper) != true)
            return false

        val parentKey = parentScheduleManager.parent?.parentKey

        if (taskData.parentKey == parentKey)
            return true

        fun CreateTaskViewModel.ParentKey.getProjectId() = when (this) {
            is CreateTaskViewModel.ParentKey.Project -> projectId
            is CreateTaskViewModel.ParentKey.Task -> findTaskData(this).projectId
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
        return DomainFactory.instance.updateScheduleTask(
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                scheduleDatas,
                createParameters.note,
                projectKey,
                createParameters.writeImagePath
        )
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        return DomainFactory.instance.updateChildTask(
                ExactTimeStamp.now,
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                parentTaskKey,
                createParameters.note,
                createParameters.writeImagePath
        )
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        return DomainFactory.instance.updateRootTask(
                data.dataId,
                SaveService.Source.GUI,
                parameters.taskKey,
                createParameters.name,
                createParameters.note,
                projectKey,
                createParameters.writeImagePath
        )
    }
}