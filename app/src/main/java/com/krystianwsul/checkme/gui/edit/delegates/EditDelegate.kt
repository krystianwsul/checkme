package com.krystianwsul.checkme.gui.edit.delegates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import arrow.core.curried
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.*
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.plusAssign

abstract class EditDelegate(
    compositeDisposable: CompositeDisposable,
    private val storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
) {

    companion object {

        fun fromParameters(
            parameters: EditParameters,
            data: EditViewModel.MainData,
            savedInstanceState: Bundle?,
            compositeDisposable: CompositeDisposable,
            storeParentKey: (EditViewModel.ParentKey?, Boolean) -> Unit,
        ): EditDelegate {
            return when (parameters) {
                is EditParameters.Copy -> ::CopyExistingTaskEditDelegate.curried()(parameters)
                is EditParameters.Edit -> ::EditExistingTaskEditDelegate.curried()(parameters)
                is EditParameters.Join -> ::JoinTasksEditDelegate.curried()(parameters)
                is EditParameters.Create, is EditParameters.Share, is EditParameters.Shortcut, EditParameters.None ->
                    ::CreateTaskEditDelegate.curried()(parameters)
            }(data)(savedInstanceState)(compositeDisposable)(storeParentKey)
        }

        fun Single<TaskKey.Root>.toCreateResult() = map<CreateResult>(CreateResult::Task)
        fun Single<CreateResult>.applyCreatedTaskKey() = doOnSuccess { EditActivity.createdTaskKey = it.taskKey }
    }

    fun newData(data: EditViewModel.MainData) {
        this.data = data

        parentScheduleManager.parent = data.currentParent
    }

    protected val callbacks = object : ParentScheduleManager.Callbacks {

        override fun getInitialParent() = data.currentParent

        override fun storeParent(parentKey: EditViewModel.ParentKey?) =
            this@EditDelegate.storeParentKey(parentKey, false)
    }

    protected abstract var data: EditViewModel.MainData

    open val initialName: String? = null
    open val initialNote: String? = null
    open val scheduleHint: EditActivity.Hint.Schedule? = null
    open val showSaveAndOpen = false

    val customTimeDatas get() = data.customTimeDatas

    protected fun TaskKey.toParentKey() = EditViewModel.ParentKey.Task(this)

    protected fun EditActivity.Hint.toScheduleHint() = this as? EditActivity.Hint.Schedule

    val firstScheduleEntry by lazy {
        val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
            ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

        ScheduleEntry(EditViewModel.ScheduleDataWrapper.Single(ScheduleData.Single(date, timePair)))
    }

    abstract val parentScheduleManager: ParentScheduleManager

    val adapterItemObservable by lazy {
        parentScheduleManager.let {
            Observables.combineLatest(it.parentObservable, it.scheduleObservable)
        }.map { (parent, schedules) ->
            listOf(EditActivity.Item.Parent) +
                    listOfNotNull(
                        parent.value
                            ?.projectUsers
                            ?.takeIf { it.size > 1 && schedules.isNotEmpty() }
                            ?.let { EditActivity.Item.AssignTo }
                    ) +
                    schedules.map { EditActivity.Item.Schedule(it) } +
                    EditActivity.Item.NewSchedule +
                    EditActivity.Item.Note +
                    EditActivity.Item.Image
        }
            .replay(1)
            .apply { compositeDisposable += connect() }
    }

    fun checkDataChanged(editImageState: EditImageState, name: String, note: String?): Boolean {
        if (parentScheduleManager.changed) return true

        if (checkImageChanged(editImageState)) return true

        return checkNameNoteChanged(name, note)
    }

    protected open fun checkImageChanged(editImageState: EditImageState) = editImageState != EditImageState.None

    protected open fun checkNameNoteChanged(name: String, note: String?) = name.isNotEmpty() || !note.isNullOrEmpty()

    protected fun checkNameNoteChanged(
        taskData: EditViewModel.TaskData,
        name: String,
        note: String?,
    ) = name != taskData.name || note != taskData.note

    fun getError(scheduleEntry: ScheduleEntry): ScheduleError? {
        if (scheduleEntry.scheduleDataWrapper !is EditViewModel.ScheduleDataWrapper.Single) return null

        if (skipScheduleCheck(scheduleEntry)) return null

        val date = scheduleEntry.scheduleDataWrapper
            .scheduleData
            .date

        if (date > Date.today()) return null

        if (date < Date.today()) return ScheduleError.DATE

        val hourMinute = scheduleEntry.scheduleDataWrapper
            .timePair
            .run {
                customTimeKey?.let { data.customTimeDatas.getValue(it) }
                    ?.hourMinutes
                    ?.getValue(date.dayOfWeek)
                    ?: hourMinute!!
            }

        if (hourMinute <= HourMinute.now) return ScheduleError.TIME

        return null
    }

    protected open fun skipScheduleCheck(scheduleEntry: ScheduleEntry): Boolean = false

    private val scheduleOffset
        get() = adapterItemObservable.getCurrentValue().indexOfFirst { it is EditActivity.Item.Schedule }

    fun setSchedule(adapterPosition: Int, scheduleDialogData: ScheduleDialogData) {
        val schedulePosition = adapterPosition - scheduleOffset

        val oldId = if (schedulePosition < parentScheduleManager.schedules.size) {
            parentScheduleManager.schedules[schedulePosition].id
        } else {
            null
        }

        val scheduleEntry = scheduleDialogData.toScheduleEntry(oldId)

        parentScheduleManager.setSchedule(schedulePosition, scheduleEntry)
    }

    fun removeSchedule(adapterPosition: Int) =
        parentScheduleManager.removeSchedule(adapterPosition - scheduleOffset)

    open fun showAllRemindersDialog(): Boolean? { // null = no, true/false = plural
        check(data.showAllInstancesDialog == null)

        return null
    }

    fun setParentTask(taskKey: TaskKey) = storeParentKey(EditViewModel.ParentKey.Task(taskKey), true)

    fun createTask(createParameters: CreateParameters, allReminders: Boolean?): Single<CreateResult> {
        check((allReminders != null) == (showAllRemindersDialog() != null))

        val projectId = (parentScheduleManager.parent?.parentKey as? EditViewModel.ParentKey.Project)?.projectId
        val assignedTo = parentScheduleManager.assignedTo

        val sharedProjectParameters = if (projectId == null) {
            check(assignedTo.isEmpty())

            null
        } else {
            SharedProjectParameters(projectId, assignedTo)
        }

        return when {
            parentScheduleManager.schedules.isNotEmpty() -> createTaskWithSchedule(
                createParameters,
                parentScheduleManager.schedules.map { it.scheduleDataWrapper.scheduleData },
                sharedProjectParameters,
                allReminders,
            )
            parentScheduleManager.parent?.parentKey is EditViewModel.ParentKey.Task -> {
                check(sharedProjectParameters == null)
                check(allReminders == null)

                val parentTaskKey = parentScheduleManager.parent!!
                    .parentKey
                    .let { it as EditViewModel.ParentKey.Task }
                    .taskKey

                createTaskWithParent(createParameters, parentTaskKey)
            }
            else -> {
                check(assignedTo.isEmpty())
                check(allReminders == null)

                createTaskWithoutReminder(createParameters, projectId)
            }
        }
    }

    abstract fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        sharedProjectParameters: SharedProjectParameters?,
        allReminders: Boolean?,
    ): Single<CreateResult>

    abstract fun createTaskWithParent(createParameters: CreateParameters, parentTaskKey: TaskKey): Single<CreateResult>

    abstract fun createTaskWithoutReminder(
        createParameters: CreateParameters,
        sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult>

    fun saveState() = parentScheduleManager.saveState()

    class CreateParameters(
        val name: String,
        val note: String?,
        val imagePath: Pair<String, Uri>?,
    ) {

        // todo add image make default params
        constructor(name: String) : this(name, null, null)
    }

    enum class ScheduleError(@StringRes val resource: Int) {

        DATE(R.string.error_date), TIME(R.string.error_time)
    }

    class SharedProjectParameters(val key: ProjectKey.Shared, val assignedTo: Set<UserKey>)

    sealed class CreateResult {

        abstract val taskKey: TaskKey.Root
        abstract val intent: Intent

        class Task(override val taskKey: TaskKey.Root) : CreateResult() {

            override val intent get() = ShowTaskActivity.newIntent(taskKey)
        }

        class Instance(private val instanceKey: InstanceKey) : CreateResult() {

            override val taskKey = instanceKey.taskKey as TaskKey.Root

            override val intent get() = ShowInstanceActivity.getIntent(MyApplication.instance, instanceKey)
        }
    }
}