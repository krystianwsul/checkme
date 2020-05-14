package com.krystianwsul.checkme.gui.edit.delegates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CreateTaskEditDelegate(
        private val parameters: EditParameters,
        override var data: EditViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, EditImageState>?
) : EditDelegate(savedStates?.third) {

    override val initialName: String?
    override val scheduleHint: EditActivity.Hint.Schedule?
    override val initialState: ParentScheduleState

    init {
        when (parameters) {
            is EditParameters.Create -> {
                initialName = parameters.nameHint
                scheduleHint = parameters.hint?.toScheduleHint()

                val initialParentKey = parameters.hint?.toParentKey()
                initialState = savedStates?.first ?: parameters.parentScheduleState
                        ?: ParentScheduleState(
                        initialParentKey,
                        listOfNotNull(firstScheduleEntry.takeIf { initialParentKey !is EditViewModel.ParentKey.Task && data.defaultReminder })
                )
            }
            is EditParameters.Share -> {
                initialName = parameters.nameHint
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint?.toParentKey()
                initialState = savedStates?.first ?: ParentScheduleState(
                        initialParentKey,
                        listOfNotNull(firstScheduleEntry.takeIf { initialParentKey == null && data.defaultReminder })
                )
            }
            is EditParameters.Shortcut -> {
                initialName = null
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint.toParentKey()
                initialState = savedStates?.first
                        ?: ParentScheduleState.create(initialParentKey)
            }
            EditParameters.None -> {
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

    override val parentScheduleManager = ParentMultiScheduleManager(savedStates?.second, initialState, parentLookup)

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
                                ?.value
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        if (parameters is EditParameters.Share)
            ShortcutManager.addShortcut(parentTaskKey)

        return DomainFactory.instance
                .createChildTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        createParameters.note,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value
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
                                ?.value
                )
                .also { EditActivity.createdTaskKey = it }
    }
}