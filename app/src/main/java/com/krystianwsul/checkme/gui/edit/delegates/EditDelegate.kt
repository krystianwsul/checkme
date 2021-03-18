package com.krystianwsul.checkme.gui.edit.delegates

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import arrow.syntax.function.invoke
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.*
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.plusAssign

abstract class EditDelegate(savedEditImageState: EditImageState?, compositeDisposable: CompositeDisposable) {

    companion object {

        private const val IMAGE_URL_KEY = "imageUrl"

        fun fromParameters(
                parameters: EditParameters,
                data: EditViewModel.Data,
                savedInstanceState: Bundle?,
                compositeDisposable: CompositeDisposable,
        ): EditDelegate {
            val savedEditImageState = savedInstanceState?.getSerializable(IMAGE_URL_KEY) as? EditImageState

            return when (parameters) {
                is EditParameters.Copy -> (::CopyExistingTaskEditDelegate)(parameters)
                is EditParameters.Edit -> (::EditExistingTaskEditDelegate)(parameters)
                is EditParameters.Join -> (::JoinTasksEditDelegate)(parameters)
                is EditParameters.Create, is EditParameters.Share, is EditParameters.Shortcut, EditParameters.None ->
                    (::CreateTaskEditDelegate)(parameters)
            }(data, savedInstanceState, savedEditImageState, compositeDisposable)
        }

        fun Single<TaskKey>.toCreateResult() = map<CreateResult>(CreateResult::Task)!!
        fun Single<CreateResult>.applyCreatedTaskKey() = doOnSuccess { EditActivity.createdTaskKey = it.taskKey }!!
    }

    fun newData(data: EditViewModel.Data) {
        this.data = data

        tmpParentTaskKey?.let {
            check(parentScheduleManager.trySetParentTask(it))
            tmpParentTaskKey = null
        }
    }

    protected abstract var data: EditViewModel.Data

    private var tmpParentTaskKey: TaskKey? = null

    open val initialName: String? = null
    open val initialNote: String? = null
    open val scheduleHint: EditActivity.Hint.Schedule? = null
    open val showSaveAndOpen = false

    val parentTreeDatas get() = data.parentTreeDatas
    val customTimeDatas get() = data.customTimeDatas

    protected fun TaskKey.toParentKey() = EditViewModel.ParentKey.Task(this)

    protected fun EditActivity.Hint.toParentKey(): EditViewModel.ParentKey? {
        return when (this) {
            is EditActivity.Hint.Schedule -> null
            is EditActivity.Hint.Task -> EditViewModel.ParentKey.Task(taskKey)
            is EditActivity.Hint.Project -> EditViewModel.ParentKey.Project(projectKey)
        }
    }

    protected fun EditActivity.Hint.toScheduleHint() = this as? EditActivity.Hint.Schedule

    val firstScheduleEntry by lazy {
        val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
                ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

        ScheduleEntry(EditViewModel.ScheduleDataWrapper.Single(ScheduleData.Single(date, timePair)))
    }

    abstract val parentScheduleManager: ParentScheduleManager

    open val imageUrl = BehaviorRelay.createDefault(savedEditImageState ?: EditImageState.None)!!

    protected val parentLookup by lazy { ParentLookup() }

    val adapterItemObservable by lazy {
        parentScheduleManager.let {
            Observables.combineLatest(it.parentObservable, it.scheduleObservable)
        }.map { (parent, schedules) ->
            listOf(EditActivity.Item.Parent) +
                    listOfNotNull(
                            parent.value
                                    ?.projectUsers
                                    ?.takeIf { it.isNotEmpty() && schedules.isNotEmpty() }
                                    ?.let { EditActivity.Item.AssignTo }
                    ) +
                    schedules.map { EditActivity.Item.Schedule(it) } +
                    EditActivity.Item.NewSchedule +
                    EditActivity.Item.Note +
                    EditActivity.Item.Image
        }
                .replay(1)!!
                .apply { compositeDisposable += connect() }
    }

    fun checkDataChanged(name: String, note: String?): Boolean {
        if (parentScheduleManager.changed) return true

        if (checkImageChanged()) return true

        return checkNameNoteChanged(name, note)
    }

    protected open fun checkImageChanged() = imageUrl.value != EditImageState.None

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

    inner class ParentLookup {

        fun findTaskData(parentKey: EditViewModel.ParentKey) = tryFindTaskData(parentKey)!!

        fun tryFindTaskData(parentKey: EditViewModel.ParentKey): EditViewModel.ParentTreeData? {
            fun helper(
                    taskDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>,
                    parentKey: EditViewModel.ParentKey,
            ): EditViewModel.ParentTreeData? {
                if (taskDatas.containsKey(parentKey))
                    return taskDatas.getValue(parentKey)

                return taskDatas.values
                        .mapNotNull { helper(it.parentTreeDatas, parentKey) }
                        .singleOrNull()
            }

            return helper(data.parentTreeDatas, parentKey)
        }
    }

    open fun showAllRemindersDialog(): Boolean? { // null = no, true/false = plural
        check(data.showAllInstancesDialog == null)

        return null
    }

    fun setParentTask(taskKey: TaskKey) {
        check(tmpParentTaskKey == null)

        if (!parentScheduleManager.trySetParentTask(taskKey))
            tmpParentTaskKey = taskKey
    }

    fun createTask(createParameters: CreateParameters): Single<CreateResult> {
        check(createParameters.allReminders || showAllRemindersDialog() != null)

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
            )
            parentScheduleManager.parent?.parentKey is EditViewModel.ParentKey.Task -> {
                check(sharedProjectParameters == null)

                val parentTaskKey = (parentScheduleManager.parent!!.parentKey as EditViewModel.ParentKey.Task).taskKey

                createTaskWithParent(createParameters, parentTaskKey)
            }
            else -> {
                check(assignedTo.isEmpty())

                createTaskWithoutReminder(createParameters, projectId)
            }
        }
    }

    abstract fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            sharedProjectParameters: SharedProjectParameters?,
    ): Single<CreateResult>

    abstract fun createTaskWithParent(createParameters: CreateParameters, parentTaskKey: TaskKey): Single<CreateResult>

    abstract fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            sharedProjectKey: ProjectKey.Shared?,
    ): Single<CreateResult>

    fun saveState(outState: Bundle) {
        parentScheduleManager.saveState(outState)
        outState.putSerializable(IMAGE_URL_KEY, imageUrl.value!!)
    }

    class CreateParameters(val name: String, val note: String?, val allReminders: Boolean)

    enum class ScheduleError(@StringRes val resource: Int) {

        DATE(R.string.error_date), TIME(R.string.error_time)
    }

    class SharedProjectParameters(val key: ProjectKey.Shared, val assignedTo: Set<UserKey>)

    sealed class CreateResult {

        abstract val taskKey: TaskKey
        abstract val intent: Intent

        class Task(override val taskKey: TaskKey) : CreateResult() {

            override val intent get() = ShowTaskActivity.newIntent(taskKey)
        }

        class Instance(private val instanceKey: InstanceKey) : CreateResult() {

            override val taskKey = instanceKey.taskKey

            override val intent get() = ShowInstanceActivity.getIntent(MyApplication.instance, instanceKey)
        }
    }
}