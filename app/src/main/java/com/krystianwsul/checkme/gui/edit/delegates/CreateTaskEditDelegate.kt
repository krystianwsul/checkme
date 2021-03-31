package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.domainmodel.extensions.createChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createRootTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleRootTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CreateTaskEditDelegate(
        private val parameters: EditParameters,
        override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        compositeDisposable: CompositeDisposable,
        storeParent: (EditViewModel.ParentTreeData?) -> Unit,
) : EditDelegate(compositeDisposable, storeParent) {

    override val initialName: String?
    override val scheduleHint: EditActivity.Hint.Schedule?
    override val showSaveAndOpen = true

    override val parentScheduleManager: ParentScheduleManager

    init {
        val initialStateGetter: () -> ParentScheduleState

        when (parameters) {
            is EditParameters.Create -> {
                initialName = parameters.nameHint
                scheduleHint = parameters.hint?.toScheduleHint()

                val initialParentKey = parameters.hint?.toParentKey()
                initialStateGetter = {
                    parameters.parentScheduleState ?: ParentScheduleState(
                            initialParentKey,
                            listOfNotNull(
                                    firstScheduleEntry.takeIf {
                                        initialParentKey !is EditViewModel.ParentKey.Task
                                                && Preferences.addDefaultReminder
                                                && parameters.showFirstSchedule
                                    }
                            ),
                            setOf()
                    )
                }
            }
            is EditParameters.Share -> {
                initialName = parameters.nameHint
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint?.toParentKey()
                initialStateGetter = {
                    ParentScheduleState(
                            initialParentKey,
                            listOfNotNull(
                                    firstScheduleEntry.takeIf {
                                        initialParentKey == null && Preferences.addDefaultReminder
                                    }
                            ),
                            setOf()
                    )
                }
            }
            is EditParameters.Shortcut -> {
                initialName = null
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint.toParentKey()
                initialStateGetter = {
                    ParentScheduleState.create(initialParentKey, setOf())
                }
            }
            EditParameters.None -> {
                initialName = null
                scheduleHint = null

                initialStateGetter = {
                    ParentScheduleState(
                            null,
                            listOfNotNull(firstScheduleEntry.takeIf { Preferences.addDefaultReminder }),
                            setOf()
                    )
                }
            }
            else -> throw IllegalArgumentException()
        }

        parentScheduleManager = ParentMultiScheduleManager(
                savedInstanceState,
                initialStateGetter,
                parentLookup,
                callbacks,
        )
    }

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
        )
                .observeOn(AndroidSchedulers.mainThread())
                .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        if (parameters is EditParameters.Share) ShortcutManager.addShortcut(parentTaskKey)

        return AndroidDomainUpdater.createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTaskKey,
                createParameters.name,
                createParameters.note,
                createParameters.editImageState
                        .writeImagePath
                        ?.value,
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
        )
                .observeOn(AndroidSchedulers.mainThread())
                .applyCreatedTaskKey()
    }
}