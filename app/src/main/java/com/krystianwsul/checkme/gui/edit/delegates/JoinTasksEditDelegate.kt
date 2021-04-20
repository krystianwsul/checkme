package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createJoinChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class JoinTasksEditDelegate(
        private val parameters: EditParameters.Join,
        override var data: EditViewModel.MainData,
        savedInstanceState: Bundle?,
        compositeDisposable: CompositeDisposable,
        storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : EditDelegate(compositeDisposable, storeParentKey) {

    override val scheduleHint = parameters.hint?.toScheduleHint()

    private val taskKeys = parameters.joinables.map { it.taskKey }
    private val instanceKeys = parameters.joinables.mapNotNull { it.instanceKey }

    private fun initialStateGetter(): ParentScheduleState {
        val schedule = if (parameters.hint is EditActivity.Hint.Task) {
            null
        } else {
            firstScheduleEntry.takeIf { Preferences.addDefaultReminder }
        }

        return ParentScheduleState(listOfNotNull(schedule), setOf())
    }

    override val parentScheduleManager = ParentMultiScheduleManager(
            savedInstanceState,
            this::initialStateGetter,
            callbacks,
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
        return AndroidDomainUpdater.createScheduleJoinTopLevelTask(
                DomainListenerManager.NotificationType.All,
                createParameters.name,
                scheduleDatas,
                parameters.joinables,
                createParameters.note,
                sharedProjectParameters,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
                createParameters.allReminders,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .toCreateResult()
                .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.createJoinChildTask(
                DomainListenerManager.NotificationType.All,
                parentTaskKey,
                createParameters.name,
                taskKeys,
                createParameters.note,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
                instanceKeys,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .toCreateResult()
                .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.createJoinTopLevelTask(
                DomainListenerManager.NotificationType.All,
                createParameters.name,
                taskKeys,
                createParameters.note,
                sharedProjectKey,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
                instanceKeys,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .toCreateResult()
                .applyCreatedTaskKey()
    }
}