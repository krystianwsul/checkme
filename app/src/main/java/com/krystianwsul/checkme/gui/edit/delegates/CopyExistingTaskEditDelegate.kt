package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.createChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createRootTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleRootTask
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CopyExistingTaskEditDelegate(
        private val parameters: EditParameters.Copy,
        data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?,
        compositeDisposable: CompositeDisposable,
) : ExistingTaskEditDelegate(data, savedInstanceState, editImageState, compositeDisposable) {

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): CreateResult {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createScheduleRootTask(
                        SaveService.Source.GUI,
                        createParameters.name,
                        scheduleDatas,
                        createParameters.note,
                        sharedProjectParameters,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.taskKey
                )
                .also { EditActivity.createdTaskKey = it }
                .toCreateResult()
    }

    override fun createTaskWithParent(createParameters: CreateParameters, parentTaskKey: TaskKey): CreateResult {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createChildTask(
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        createParameters.note,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.taskKey
                )
                .also { EditActivity.createdTaskKey = it }
                .toCreateResult()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): CreateResult {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createRootTask(
                        SaveService.Source.GUI,
                        createParameters.name,
                        createParameters.note,
                        sharedProjectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value,
                        parameters.taskKey
                )
                .also { EditActivity.createdTaskKey = it }
                .toCreateResult()
    }
}