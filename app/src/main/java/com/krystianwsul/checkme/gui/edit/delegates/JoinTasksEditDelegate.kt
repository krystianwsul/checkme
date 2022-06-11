package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createJoinChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ParentScheduleManager
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
) : EditDelegate(savedInstanceState, compositeDisposable, storeParentKey) {

    override val scheduleHint = parameters.hint
        ?.toScheduleHint()
        ?.dateTimePair

    override val defaultInitialParentScheduleState = if (parameters.hint?.showInitialSchedule != false) {
        ParentScheduleState(getDefaultSingleScheduleData())
    } else {
        ParentScheduleState.empty
    }

    private val taskKeys = parameters.joinables.map { it.taskKey }
    private val instanceKeys = parameters.joinables.mapNotNull { it.instanceKey }

    override fun showDialog(): ShowDialog {
        fun showJoinAllRemindersDialog(): Boolean {
            return when (val parent = parentScheduleManager.parent) {
                is ParentScheduleManager.Parent.Task -> {
                    check(parentScheduleManager.schedules.isEmpty())

                    return parent.topLevelTaskIsSingleSchedule
                }
                is ParentScheduleManager.Parent.Project, null -> {
                    parentScheduleManager.schedules
                        .singleOrNull()
                        ?.scheduleDataWrapper
                        ?.scheduleData is ScheduleData.Single
                }
            }
        }

        return if (showJoinAllRemindersDialog()) ShowDialog.JOIN else ShowDialog.NONE
    }

    override fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        projectParameters: ProjectParameters?,
        joinAllInstances: Boolean?,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            scheduleDatas,
            parameters.joinables,
            projectParameters,
            joinAllInstances != false,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
            .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        dialogResult: DialogResult,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.createJoinChildTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            createParameters,
            parameters.joinables,
            dialogResult.joinAllInstances != false,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
            .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
        createParameters: CreateParameters,
        projectKey: ProjectKey<*>?,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.createJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            taskKeys,
            projectKey,
            instanceKeys,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
            .applyCreatedTaskKey()
    }
}