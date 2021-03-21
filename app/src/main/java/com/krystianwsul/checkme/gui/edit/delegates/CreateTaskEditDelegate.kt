package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.domainmodel.extensions.createChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createRootTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleRootTask
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CreateTaskEditDelegate(
        private val parameters: EditParameters,
        override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        compositeDisposable: CompositeDisposable,
) : EditDelegate(compositeDisposable) {

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
        )
    }

    override fun getInitialEditImageState(savedEditImageState: EditImageState?): EditImageState {
        return when {
            savedEditImageState?.dontOverwrite == true -> savedEditImageState
            (parameters as? EditParameters.Share)?.uri != null -> parameters.uri!!
                    .toString()
                    .let { EditImageState.Selected(it, it) }
            savedEditImageState != null -> savedEditImageState
            else -> EditImageState.None
        }
    }

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createScheduleRootTask(
                        DomainListenerManager.NotificationType.All,
                        createParameters.name,
                        scheduleDatas,
                        createParameters.note,
                        sharedProjectParameters,
                        createParameters.editImageState
                                .writeImagePath
                                ?.value,
                )
                .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        if (parameters is EditParameters.Share) ShortcutManager.addShortcut(parentTaskKey)

        return DomainFactory.instance
                .createChildTask(
                        DomainListenerManager.NotificationType.All,
                        parentTaskKey,
                        createParameters.name,
                        createParameters.note,
                        createParameters.editImageState
                                .writeImagePath
                                ?.value,
                )
                .applyCreatedTaskKey()
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult> {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createRootTask(
                        DomainListenerManager.NotificationType.All,
                        createParameters.name,
                        createParameters.note,
                        sharedProjectKey,
                        createParameters.editImageState
                                .writeImagePath
                                ?.value,
                )
                .applyCreatedTaskKey()
    }
}