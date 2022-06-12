package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.updateChildTask
import com.krystianwsul.checkme.domainmodel.extensions.updateScheduleTask
import com.krystianwsul.checkme.domainmodel.extensions.updateTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class EditExistingTaskEditDelegate(
    private val parameters: EditParameters.Edit,
    data: EditViewModel.MainData,
    savedInstanceState: Bundle?,
    compositeDisposable: CompositeDisposable,
    storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : ExistingTaskEditDelegate(data, savedInstanceState, compositeDisposable, storeParentKey) {

    override fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        projectParameters: ProjectParameters?,
        joinAllInstances: Boolean?,
    ): Single<CreateResult> {
        check(joinAllInstances == null)

        return AndroidDomainUpdater.updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            parameters.taskKey,
            createParameters,
            scheduleDatas,
            projectParameters,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
    }

    override fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        dialogResult: DialogResult,
    ): Single<CreateResult> {
        check(dialogResult == DialogResult.None)

        return AndroidDomainUpdater.updateChildTask(
            DomainListenerManager.NotificationType.All,
            parameters.taskKey,
            createParameters,
            parentTaskKey,
            parameters.openedFromInstanceKey,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
    }

    override fun createTaskWithoutReminder(
        createParameters: CreateParameters,
        projectKey: ProjectKey<*>?,
    ): Single<CreateResult> {
        return AndroidDomainUpdater.updateTopLevelTask(
            DomainListenerManager.NotificationType.All,
            parameters.taskKey,
            createParameters,
            projectKey,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .toCreateResult()
    }
}