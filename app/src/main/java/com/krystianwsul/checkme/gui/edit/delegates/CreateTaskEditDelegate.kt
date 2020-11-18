package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.domainmodel.extensions.createChildTask
import com.krystianwsul.checkme.domainmodel.extensions.createRootTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleRootTask
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

class CreateTaskEditDelegate(
        private val parameters: EditParameters,
        override var data: EditViewModel.Data,
        savedInstanceState: Bundle?,
        editImageState: EditImageState?
) : EditDelegate(editImageState) {

    override val initialName: String?
    override val scheduleHint: EditActivity.Hint.Schedule?
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
                                                && data.defaultReminder
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
                                    firstScheduleEntry.takeIf { initialParentKey == null && data.defaultReminder }
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
                            listOfNotNull(firstScheduleEntry.takeIf { data.defaultReminder }),
                            setOf()
                    )
                }
            }
            else -> throw IllegalArgumentException()
        }

        parentScheduleManager = ParentMultiScheduleManager(
                savedInstanceState,
                initialStateGetter,
                parentLookup
        )
    }

    override fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createScheduleRootTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        createParameters.name,
                        scheduleDatas,
                        createParameters.note,
                        sharedProjectParameters,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithParent(createParameters: CreateParameters, parentTaskKey: TaskKey): TaskKey {
        check(createParameters.allReminders)

        if (parameters is EditParameters.Share) ShortcutManager.addShortcut(parentTaskKey)

        return DomainFactory.instance
                .createChildTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        parentTaskKey,
                        createParameters.name,
                        createParameters.note,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value
                )
                .also { EditActivity.createdTaskKey = it }
    }

    override fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): TaskKey {
        check(createParameters.allReminders)

        return DomainFactory.instance
                .createRootTask(
                        data.dataId,
                        SaveService.Source.GUI,
                        createParameters.name,
                        createParameters.note,
                        sharedProjectKey,
                        imageUrl.value!!
                                .writeImagePath
                                ?.value
                )
                .also { EditActivity.createdTaskKey = it }
    }
}