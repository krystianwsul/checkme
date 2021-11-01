package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CopyExistingTaskEditDelegate(
    private val parameters: EditParameters.Copy,
    data: EditViewModel.MainData,
    savedInstanceState: Bundle?,
    compositeDisposable: CompositeDisposable,
    storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : ExistingTaskEditDelegate(data, savedInstanceState, compositeDisposable, storeParentKey) {

    override fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        sharedProjectParameters: SharedProjectParameters?,
        joinAllInstances: Boolean?,
    ): Single<CreateResult> {
        check(joinAllInstances == null)

        return AndroidDomainUpdater.createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            scheduleDatas,
            sharedProjectParameters,
            parameters.taskKey,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        dialogResult: DialogResult,
    ): Single<CreateResult> {
        check(dialogResult == DialogResult.None)

        return CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            createParameters,
            parameters.taskKey,
        )
            .perform(AndroidDomainUpdater)
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
        createParameters: CreateParameters,
        sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.createTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            sharedProjectKey,
            parameters.taskKey,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }
}