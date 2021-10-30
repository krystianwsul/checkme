package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createTopLevelTask
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.utils.exhaustive
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CreateTaskEditDelegate(
    private val parameters: EditParameters.CreateDelegateParameters,
    override var data: EditViewModel.MainData,
    savedInstanceState: Bundle?,
    compositeDisposable: CompositeDisposable,
    storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) : EditDelegate(compositeDisposable, storeParentKey) {

    override val initialName: String?
    override val scheduleHint: EditParentHint.Schedule?
    override val showSaveAndOpen = true

    override val parentScheduleManager: ParentScheduleManager

    init {
        val initialStateGetter: () -> ParentScheduleState

        when (parameters) {
            is EditParameters.Create -> {
                initialName = parameters.nameHint
                scheduleHint = parameters.hint?.toScheduleHint()

                initialStateGetter = {
                    parameters.parentScheduleState ?: ParentScheduleState(
                        listOfNotNull(
                            firstScheduleEntry.takeIf {
                                parameters.hint?.showInitialSchedule != false &&
                                        Preferences.addDefaultReminder &&
                                        parameters.showFirstSchedule
                            }
                        ),
                        setOf()
                    )
                }
            }
            is EditParameters.MigrateDescription -> {
                initialName = data.parentTaskDescription!!
                scheduleHint = null

                initialStateGetter = { ParentScheduleState.create(setOf()) }
            }
            is EditParameters.Share -> {
                initialName = parameters.nameHint
                scheduleHint = null

                val initialParentKey = parameters.parentTaskKeyHint?.toParentKey()
                initialStateGetter = {
                    ParentScheduleState(
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

                initialStateGetter = { ParentScheduleState.create(setOf()) }
            }
            EditParameters.None -> {
                initialName = null
                scheduleHint = null

                initialStateGetter = {
                    ParentScheduleState(
                        listOfNotNull(firstScheduleEntry.takeIf { Preferences.addDefaultReminder }),
                        setOf(),
                    )
                }
            }
        }.exhaustive()

        parentScheduleManager = ParentMultiScheduleManager(
            savedInstanceState,
            initialStateGetter,
            callbacks,
        )
    }

    override fun skipScheduleCheck(scheduleEntry: ScheduleEntry): Boolean {
        if (parameters !is EditParameters.Create) return false

        val scheduleHint = parameters.hint as? EditParentHint.Schedule ?: return false
        val projectParentKey = scheduleHint.toParentKey() ?: return false

        if (parentScheduleManager.parent?.parentKey != projectParentKey) return false

        val singleScheduleData = scheduleEntry.scheduleDataWrapper
            .scheduleData
            .let { it as? ScheduleData.Single }
            ?: return false

        if (singleScheduleData.date != scheduleHint.date) return false
        if (singleScheduleData.timePair != scheduleHint.timePair) return false

        return true
    }

    override fun showDialog(): ShowDialog {
        val parent = parentScheduleManager.parent ?: return ShowDialog.NONE

        val parentTaskKey = parent.parentKey
            .let { it as? EditViewModel.ParentKey.Task }
            ?.taskKey
            ?: return ShowDialog.NONE

        if (parentTaskKey != (parameters as? EditParameters.Create)?.hint?.instanceKey?.taskKey) {
            check(parent.hasMultipleInstances == null)

            return ShowDialog.NONE
        }

        return if (parent.hasMultipleInstances!!) ShowDialog.ADD else ShowDialog.NONE
    }

    override fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        sharedProjectParameters: SharedProjectParameters?,
        joinAllReminders: Boolean?,
    ): Single<CreateResult> {
        check(joinAllReminders == null)

        return AndroidDomainUpdater.createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            scheduleDatas,
            sharedProjectParameters,
        )
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }

    override fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        addToAllInstances: Boolean?,
    ): Single<CreateResult> {
        if (parameters is EditParameters.Share) ShortcutManager.addShortcut(parentTaskKey)

        val parentParameter = if (addToAllInstances == false) {
            val parentInstanceKey = (parameters as EditParameters.Create).hint!!.instanceKey!!
            check(parentInstanceKey.taskKey == parentTaskKey)

            CreateChildTaskDomainUpdate.Parent.Instance(parentInstanceKey)
        } else {
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey)
        }

        return CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            parentParameter,
            createParameters,
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
        )
            .observeOn(AndroidSchedulers.mainThread())
            .applyCreatedTaskKey()
    }
}