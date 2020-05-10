package com.krystianwsul.checkme.gui.tasks.create.delegates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskImageState
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskParameters
import com.krystianwsul.checkme.gui.tasks.create.ParentScheduleState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CreateCreateTaskDelegate(
        private val parameters: CreateTaskParameters,
        override var data: CreateTaskViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, CreateTaskImageState>?
) : CreateTaskDelegate(savedStates?.third) {

    override val initialName: String?
    override val scheduleHint: CreateTaskActivity.Hint.Schedule?
    override val initialState: ParentScheduleState

    init {
        when (parameters) {
            is CreateTaskParameters.Create -> {
                initialName = parameters.nameHint
                scheduleHint = parameters.hint?.toScheduleHint()

                val initialParentKey = parameters.hint?.toParentKey()
                initialState = savedStates?.first ?: parameters.parentScheduleState
                        ?: ParentScheduleState(
                        initialParentKey,
                        listOfNotNull(firstScheduleEntry.takeIf { initialParentKey !is CreateTaskViewModel.ParentKey.Task && data.defaultReminder })
                )
            }
            is CreateTaskParameters.Share -> {
                initialName = parameters.nameHint
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint?.toParentKey()
                initialState = savedStates?.first ?: ParentScheduleState(
                        initialParentKey,
                        listOfNotNull(firstScheduleEntry.takeIf { initialParentKey == null && data.defaultReminder })
                )
            }
            is CreateTaskParameters.Shortcut -> {
                initialName = null
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint.toParentKey()
                initialState = savedStates?.first
                        ?: ParentScheduleState.create(initialParentKey)
            }
            CreateTaskParameters.None -> {
                initialName = null
                scheduleHint = null

                initialState = savedStates?.first ?: ParentScheduleState(
                        null,
                        listOfNotNull(firstScheduleEntry.takeIf { data.defaultReminder })
                )
            }
            else -> throw IllegalArgumentException()
        }
    }

    override val parentScheduleManager = getParentScheduleManager(savedStates?.second)

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
                        createParameters.writeImagePath?.value
                )
                .also { CreateTaskActivity.createdTaskKey = it }
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        if (parameters is CreateTaskParameters.Share)
            ShortcutManager.addShortcut(parentTaskKey)

        return DomainFactory.instance
                .createChildTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        createParameters.note,
                        createParameters.writeImagePath?.value
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
                        createParameters.writeImagePath?.value
                )
                .also { CreateTaskActivity.createdTaskKey = it }
    }
}