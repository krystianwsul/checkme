package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createJoinChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createJoinRootTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleJoinRootTask
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class JoinTasksEditDelegate(
        private val parameters: EditParameters.Join,
        override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?,
        compositeDisposable: CompositeDisposable,
) : EditDelegate(editImageState, compositeDisposable) {

    override val scheduleHint = parameters.hint?.toScheduleHint()

    private val taskKeys = parameters.joinables.map { it.taskKey }
    private val instanceKeys = parameters.joinables.mapNotNull { it.instanceKey }

    private fun initialStateGetter(): ParentScheduleState {
        val (initialParentKey, schedule) = if (parameters.hint is EditActivity.Hint.Task) {
            Pair(parameters.hint.toParentKey(), null)
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

        return ParentScheduleState(
                initialParentKey,
                listOfNotNull(schedule),
                setOf()
        )
    }

    override val parentScheduleManager = ParentMultiScheduleManager(
            savedInstanceState,
            this::initialStateGetter,
            parentLookup
    )

    override fun showAllRemindersDialog(): Boolean? {
        if (!data.showAllInstancesDialog!!) return null

        val schedule = parentScheduleManager.schedules
                .singleOrNull()
                ?: return null

        return if (schedule.scheduleDataWrapper.scheduleData is ScheduleData.Single)
            true
        else
            null
    }

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): Single<CreateResult> {
        return DomainFactory.instance
                .createScheduleJoinRootTask(
                        DomainListenerManager.NotificationType.All,
                        SaveService.Source.GUI,
                        createParameters.name,
                        scheduleDatas,
                        parameters.joinables,
                        createParameters.note,
                        sharedProjectParameters,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        createParameters.allReminders,
                )
                .toCreateResult()
                .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createJoinChildTask(
                        DomainListenerManager.NotificationType.All,
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        taskKeys,
                        createParameters.note,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        instanceKeys,
                )
                .toCreateResult()
                .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createJoinRootTask(
                        DomainListenerManager.NotificationType.All,
                        SaveService.Source.GUI,
                        createParameters.name,
                        taskKeys,
                        createParameters.note,
                        sharedProjectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        instanceKeys,
                )
                .toCreateResult()
                .applyCreatedTaskKey()
    }
}