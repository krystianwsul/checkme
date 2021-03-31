package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createRootTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleRootTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
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
        data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        compositeDisposable: CompositeDisposable,
        storeParent: (EditViewModel.ParentTreeData?) -> Unit,
) : ExistingTaskEditDelegate(data, savedInstanceState, compositeDisposable, storeParent) {

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                createParameters.name,
                scheduleDatas,
                createParameters.note,
                sharedProjectParameters,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
                parameters.taskKey,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTaskKey,
                createParameters.name,
                createParameters.note,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
                parameters.taskKey,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return AndroidDomainUpdater.createRootTask(
                DomainListenerManager.NotificationType.All,
                createParameters.name,
                createParameters.note,
                sharedProjectKey,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
                parameters.taskKey,
        )
                .observeOn(AndroidSchedulers.mainThread())
                .applyCreatedTaskKey()
    }
}