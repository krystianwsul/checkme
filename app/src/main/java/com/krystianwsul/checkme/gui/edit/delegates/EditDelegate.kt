package com.krystianwsul.checkme.gui.edit.delegates

import android.os.Bundle
import androidx.annotation.StringRes
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey

abstract class EditDelegate(editImageState: EditImageState?) {

    companion object {

        const val KEY_INITIAL_STATE = "initialState" // todo group task move into manager
        const val KEY_STATE = "state"
        private const val IMAGE_URL_KEY = "imageUrl"

        fun fromParameters(
                parameters: EditParameters,
                data: EditViewModel.Data,
                savedInstanceState: Bundle?
        ): EditDelegate {
            val savedStates = savedInstanceState?.takeIf { it.containsKey(KEY_INITIAL_STATE) }?.run {
                Triple<ParentScheduleState, ParentScheduleState, EditImageState>(
                        getParcelable(KEY_INITIAL_STATE)!!,
                        getParcelable(KEY_STATE)!!,
                        getSerializable(IMAGE_URL_KEY) as EditImageState
                )
            }

            return when (parameters) {
                is EditParameters.Copy -> CopyExistingTaskEditDelegate(parameters, data, savedStates)
                is EditParameters.Edit -> EditExistingTaskEditDelegate(parameters, data, savedStates)
                is EditParameters.Join -> JoinTasksEditDelegate(parameters, data, savedStates)
                is EditParameters.Create,
                is EditParameters.Share,
                is EditParameters.Shortcut,
                EditParameters.None -> CreateTaskEditDelegate(parameters, data, savedStates)
            }
        }
    }

    fun newData(data: EditViewModel.Data) {
        this.data = data
    }

    protected abstract var data: EditViewModel.Data

    open val initialName: String? = null
    open val scheduleHint: EditActivity.Hint.Schedule? = null
    open val showSaveAndOpen: Boolean = true

    val parentTreeDatas get() = data.parentTreeDatas
    val customTimeDatas get() = data.customTimeDatas

    protected fun TaskKey.toParentKey() = EditViewModel.ParentKey.Task(this)
    protected fun EditActivity.Hint.toParentKey() = (this as? EditActivity.Hint.Task)?.taskKey?.toParentKey()
    protected fun EditActivity.Hint.toScheduleHint() = this as? EditActivity.Hint.Schedule

    val firstScheduleEntry by lazy {
        val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
                ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

        ScheduleEntry(EditViewModel.ScheduleDataWrapper.Single(ScheduleData.Single(date, timePair)))
    }

    protected abstract val initialState: ParentScheduleState
    abstract val parentScheduleManager: ParentScheduleManager

    open val imageUrl = BehaviorRelay.createDefault(editImageState
            ?: EditImageState.None)

    val parentLookup by lazy { ParentLookup() }

    fun checkDataChanged(name: String, note: String?): Boolean {
        if (parentScheduleManager.changed)
            return true

        return checkNameNoteChanged(name, note)
    }

    protected open fun checkNameNoteChanged(name: String, note: String?) = name.isNotEmpty() || !note.isNullOrEmpty()

    protected fun checkNameNoteChanged(
            taskData: EditViewModel.TaskData,
            name: String,
            note: String?
    ) = name != taskData.name || note != taskData.note

    fun getError(scheduleEntry: ScheduleEntry): ScheduleError? {
        if (scheduleEntry.scheduleDataWrapper !is EditViewModel.ScheduleDataWrapper.Single)
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

    inner class ParentLookup {

        fun findTaskData(parentKey: EditViewModel.ParentKey): EditViewModel.ParentTreeData {
            fun helper(
                    taskDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>,
                    parentKey: EditViewModel.ParentKey
            ): EditViewModel.ParentTreeData? {
                if (taskDatas.containsKey(parentKey))
                    return taskDatas.getValue(parentKey)

                return taskDatas.values
                        .mapNotNull { helper(it.parentTreeDatas, parentKey) }
                        .singleOrNull()
            }

            return helper(data.parentTreeDatas, parentKey)!!
        }
    }

    fun createTask(createParameters: CreateParameters): TaskKey {
        val projectId = (parentScheduleManager.parent?.parentKey as? EditViewModel.ParentKey.Project)?.projectId

        return when {
            parentScheduleManager.schedules.isNotEmpty() -> createTaskWithSchedule(
                    createParameters,
                    parentScheduleManager.schedules.map { it.scheduleDataWrapper.scheduleData },
                    projectId
            )
            parentScheduleManager.parent?.parentKey is EditViewModel.ParentKey.Task -> {
                check(projectId == null)

                val parentTaskKey = (parentScheduleManager.parent!!.parentKey as EditViewModel.ParentKey.Task).taskKey

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
        outState.putSerializable(IMAGE_URL_KEY, imageUrl.value!!)
    }

    class CreateParameters(val name: String, val note: String?)

    enum class ScheduleError(@StringRes val resource: Int) {

        DATE(R.string.error_date), TIME(R.string.error_time)
    }
}