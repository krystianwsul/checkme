package com.krystianwsul.checkme.gui.edit.delegates

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class JoinTasksEditDelegate(
        private val parameters: EditParameters.Join,
        override var data: EditViewModel.Data,
        savedStates: Triple<ParentScheduleState, ParentScheduleState, EditImageState>?
) : EditDelegate(savedStates?.third) {

    override val scheduleHint = parameters.hint?.toScheduleHint()

    override val initialState: ParentScheduleState

    init {
        val (initialParentKey, schedule) = parameters.run {
            if (hint is EditActivity.Hint.Task) {
                Pair(hint.toParentKey(), null)
            } else {
                Pair(
                        taskKeys.map { it.projectKey }
                                .distinct()
                                .singleOrNull()
                                ?.let {
                                    (it as? ProjectKey.Shared)?.let { EditViewModel.ParentKey.Project(it) }
                                },
                        firstScheduleEntry.takeIf { data.defaultReminder }
                )
            }
        }

        initialState = savedStates?.first ?: ParentScheduleState(
                initialParentKey,
                listOfNotNull(schedule)
        )
    }

    override val parentScheduleManager = ParentMultiScheduleManager(savedStates?.second, initialState, parentLookup)

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        return DomainFactory.instance
                .createScheduleJoinRootTask(
                        ExactTimeStamp.now,
                        data.dataId,
                        SaveService.Source.GUI,
                        createParameters.name,
                        scheduleDatas,
                        parameters.taskKeys,
                        createParameters.note,
                        projectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.removeInstanceKeys
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey {
        return DomainFactory.instance
                .createJoinChildTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        parameters.taskKeys,
                        createParameters.note,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.removeInstanceKeys
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            projectKey: ProjectKey.Shared?
    ): TaskKey {
        return DomainFactory.instance
                .createJoinRootTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        createParameters.name,
                        parameters.taskKeys,
                        createParameters.note,
                        projectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.removeInstanceKeys
                )
                .also { EditActivity.createdTaskKey = it }
    }
}