package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createJoinChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ParentMultiScheduleManager
import com.krystianwsul.checkme.gui.edit.ParentScheduleState
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
        val schedule = if (parameters.hint?.showInitialSchedule == false) {
            null
        } else {
            firstScheduleEntry.takeIf { Preferences.addDefaultReminder }
        }

        return ParentScheduleState(listOfNotNull(schedule), setOf())
    }

    override val parentScheduleManager = ParentMultiScheduleManager(savedInstanceState, this::initialStateGetter, callbacks)

    override fun showDialog(): ShowDialog {
        if (!data.showJoinAllRemindersDialog!!) return ShowDialog.NONE

        // todo if parent task is present, this should instead check if the top-level task is has a single current single schedule

        parentScheduleManager.parent

        val schedule = parentScheduleManager.schedules
            .singleOrNull()
            ?: return ShowDialog.NONE

        return if (schedule.scheduleDataWrapper.scheduleData is ScheduleData.Single) ShowDialog.JOIN else ShowDialog.NONE
    }

    override fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        sharedProjectParameters: SharedProjectParameters?,
        joinAllReminders: Boolean?,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            scheduleDatas,
            parameters.joinables,
            sharedProjectParameters,
            joinAllReminders != false,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
            .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        addToAllInstances: Boolean?,
    ): Single<CreateResult> {
        check(addToAllInstances == null)

        return AndroidDomainUpdater.createJoinChildTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            createParameters,
            taskKeys,
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
        return AndroidDomainUpdater.createJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            taskKeys,
            sharedProjectKey,
            instanceKeys,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
            .applyCreatedTaskKey()
    }
}