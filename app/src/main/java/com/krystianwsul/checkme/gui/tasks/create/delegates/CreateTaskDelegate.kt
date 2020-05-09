package com.krystianwsul.checkme.gui.tasks.create.delegates

import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tasks.ScheduleEntry
import com.krystianwsul.checkme.gui.tasks.create.*
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

abstract class CreateTaskDelegate {

    companion object {

        private const val KEY_INITIAL_STATE = "initialState"
        const val KEY_STATE = "state"

        fun fromParameters(
                parameters: CreateTaskParameters,
                data: CreateTaskViewModel.Data,
                savedInstanceState: Bundle?
        ): CreateTaskDelegate {
            val savedStates = savedInstanceState?.takeIf { it.containsKey(KEY_INITIAL_STATE) }?.run {
                Pair<ParentScheduleState, ParentScheduleState>(
                        getParcelable(KEY_INITIAL_STATE)!!,
                        getParcelable(KEY_STATE)!!
                )
            }

            return when (parameters) {
                is CreateTaskParameters.Copy -> CopyCreateTaskDelegate(parameters, data, savedStates)
                is CreateTaskParameters.Edit -> EditCreateTaskDelegate(parameters, data, savedStates)
                is CreateTaskParameters.Join -> JoinCreateTaskDelegate(parameters, data, savedStates)
                is CreateTaskParameters.Create,
                is CreateTaskParameters.Share,
                is CreateTaskParameters.Shortcut,
                CreateTaskParameters.None -> CreateCreateTaskDelegate(parameters, data, savedStates)
            }
        }
    }

    fun newData(data: CreateTaskViewModel.Data) {
        this.data = data
    }

    protected abstract var data: CreateTaskViewModel.Data

    open val initialName: String? = null
    open val scheduleHint: CreateTaskActivity.Hint.Schedule? = null
    open val showSaveAndOpen: Boolean = true
    open val initialImageState: CreateTaskImageState.Existing? = null

    val parentTreeDatas get() = data.parentTreeDatas
    val customTimeDatas get() = data.customTimeDatas

    protected fun TaskKey.toParentKey() = CreateTaskViewModel.ParentKey.Task(this)
    protected fun CreateTaskActivity.Hint.toParentKey() = (this as? CreateTaskActivity.Hint.Task)?.taskKey?.toParentKey()
    protected fun CreateTaskActivity.Hint.toScheduleHint() = this as? CreateTaskActivity.Hint.Schedule

    val firstScheduleEntry by lazy {
        val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
                ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

        ScheduleEntry(CreateTaskViewModel.ScheduleDataWrapper.Single(ScheduleData.Single(date, timePair)))
    }

    protected abstract val initialState: ParentScheduleState
    abstract val parentScheduleManager: ParentScheduleManager

    protected fun getParentScheduleManager(savedState: ParentScheduleState?): ParentScheduleManager {
        val parentScheduleState = savedState ?: initialState.copy()
        val initialParent = parentScheduleState.parentKey?.let { findTaskData(it) }
        return ParentScheduleManager(parentScheduleState, initialParent)
    }

    fun checkDataChanged(name: String, note: String?): Boolean {
        if (parentScheduleManager.toState() != initialState)
            return true

        return checkNameNoteChanged(name, note)
    }

    protected open fun checkNameNoteChanged(name: String, note: String?) = name.isNotEmpty() || !note.isNullOrEmpty()

    protected fun checkNameNoteChanged(
            taskData: CreateTaskViewModel.TaskData,
            name: String,
            note: String?
    ) = name != taskData.name || note != taskData.note

    fun getError(scheduleEntry: ScheduleEntry): ScheduleError? {
        if (scheduleEntry.scheduleDataWrapper !is CreateTaskViewModel.ScheduleDataWrapper.Single)
            return null

        if (skipScheduleCheck(scheduleEntry))
            return null

        val date = scheduleEntry.scheduleDataWrapper
                .scheduleData
                .date

        if (date > Date.today())
            return null

        if (date < Date.today())
            return ScheduleError.DATE

        val hourMinute = scheduleEntry.scheduleDataWrapper
                .timePair
                .run {
                    customTimeKey?.let { data.customTimeDatas.getValue(it) }
                            ?.hourMinutes
                            ?.getValue(date.dayOfWeek)
                            ?: hourMinute!!
                }

        if (hourMinute <= HourMinute.now)
            return ScheduleError.TIME

        return null
    }

    protected open fun skipScheduleCheck(scheduleEntry: ScheduleEntry): Boolean = false

    fun findTaskData(parentKey: CreateTaskViewModel.ParentKey) =
            findTaskDataHelper(data.parentTreeDatas, parentKey).single()

    private fun findTaskDataHelper(
            taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>,
            parentKey: CreateTaskViewModel.ParentKey
    ): Iterable<CreateTaskViewModel.ParentTreeData> {
        if (taskDatas.containsKey(parentKey))
            return listOf(taskDatas.getValue(parentKey))

        return taskDatas.values
                .map { findTaskDataHelper(it.parentTreeDatas, parentKey) }
                .flatten()
    }

    fun createTask(createParameters: CreateParameters): TaskKey {
        val projectId = (parentScheduleManager.parent?.parentKey as? CreateTaskViewModel.ParentKey.Project)?.projectId

        return when {
            parentScheduleManager.schedules.isNotEmpty() -> createTaskWithSchedule(
                    createParameters,
                    parentScheduleManager.schedules.map { it.scheduleDataWrapper.scheduleData },
                    projectId
            )
            parentScheduleManager.parent?.parentKey is CreateTaskViewModel.ParentKey.Task -> {
                check(projectId == null)

                val parentTaskKey = (parentScheduleManager.parent!!.parentKey as CreateTaskViewModel.ParentKey.Task).taskKey

                createTaskWithParent(createParameters, parentTaskKey)
            }
            else -> createTaskWithoutReminder(createParameters, projectId)
        }
    }

    abstract fun createTaskWithSchedule(
            createParameters: CreateParameters,
            scheduleDatas: List<ScheduleData>,
            projectKey: ProjectKey.Shared?
    ): TaskKey

    abstract fun createTaskWithParent(
            createParameters: CreateParameters,
            parentTaskKey: TaskKey
    ): TaskKey

    abstract fun createTaskWithoutReminder(
            createParameters: CreateParameters,
            projectKey: ProjectKey.Shared?
    ): TaskKey

    fun saveState(outState: Bundle) {
        outState.putParcelable(KEY_STATE, parentScheduleManager.toState())
        outState.putParcelable(KEY_INITIAL_STATE, initialState)
    }

    class CreateParameters(
            val name: String,
            val note: String?,
            val writeImagePath: NullableWrapper<Pair<String, Uri>>?
    )

    enum class ScheduleError(@StringRes val resource: Int) {

        DATE(R.string.error_date), TIME(R.string.error_time)
    }
}