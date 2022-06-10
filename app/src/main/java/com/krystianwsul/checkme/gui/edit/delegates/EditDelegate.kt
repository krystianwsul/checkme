package com.krystianwsul.checkme.gui.edit.delegates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import arrow.core.curried
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.edit.*
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogData
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.newUuid
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.HourMinute
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
                is EditParameters.CreateDelegateParameters -> ::CreateTaskEditDelegate.curried()(parameters)
            }(data)(savedInstanceState)(compositeDisposable)(storeParentKey)
        }

        fun Single<TaskKey.Root>.toCreateResult() = map<CreateResult>(CreateResult::Task)
        fun Single<CreateResult>.applyCreatedTaskKey() = doOnSuccess { EditActivity.createdTaskKey = it.taskKey }
    }

    fun newData(data: EditViewModel.MainData) {
        this.data = data

        parentScheduleManager.setNewParent(data.currentParent)
    }

    protected val callbacks = object : ParentScheduleManager.Callbacks {

        override fun getInitialParent() = data.currentParent

        override fun storeParent(parentKey: EditViewModel.ParentKey?) =
            this@EditDelegate.storeParentKey(parentKey, false)
    }

    protected abstract var data: EditViewModel.MainData

    open val initialName: String? = null
    open val initialNote: String? = null
    open val scheduleHint: DateTimePair? = null
    open val showSaveAndOpen = false

    val customTimeDatas get() = data.customTimeDatas

    protected fun TaskKey.toParentKey() = EditViewModel.ParentKey.Task(this)

    protected fun EditParentHint.toScheduleHint() = this as? EditParentHint.Schedule

    val firstScheduleEntry by lazy {
        val dateTimePair = scheduleHint ?: HourMinute.nextHour.let { DateTimePair(it.first, it.second) }

        ScheduleEntry(ScheduleDataWrapper.Single(ScheduleData.Single(dateTimePair)))
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

    open fun showDialog() = ShowDialog.NONE

    fun setParentTask(taskKey: TaskKey) = storeParentKey(EditViewModel.ParentKey.Task(taskKey), true)

    fun createTask(createParameters: CreateParameters, dialogResult: DialogResult): Single<CreateResult> {
        dialogResult.matchesShowDialog(showDialog())

        val projectId = (parentScheduleManager.parent?.parentKey as? EditViewModel.ParentKey.Project)?.projectId
        val assignedTo = parentScheduleManager.assignedTo

        return when {
            parentScheduleManager.schedules.isNotEmpty() -> {
                val projectParameters = if (projectId == null) {
                    check(assignedTo.isEmpty())

                    null
                } else {
                    ProjectParameters(projectId, assignedTo)
                }

                createTaskWithSchedule(
                    createParameters,
                    parentScheduleManager.schedules.map { it.scheduleDataWrapper.scheduleData },
                    projectParameters,
                    dialogResult.joinAllInstances,
                )
            }
            parentScheduleManager.parent?.parentKey is EditViewModel.ParentKey.Task -> {
                check(projectId == null)

                val parentTaskKey = parentScheduleManager.parent!!
                    .parentKey
                    .let { it as EditViewModel.ParentKey.Task }
                    .taskKey

                createTaskWithParent(createParameters, parentTaskKey, dialogResult)
            }
            else -> {
                check(assignedTo.isEmpty())
                check(dialogResult == DialogResult.None)

                createTaskWithoutReminder(createParameters, projectId)
            }
        }
    }

    abstract fun createTaskWithSchedule(
        createParameters: CreateParameters,
        scheduleDatas: List<ScheduleData>,
        projectParameters: ProjectParameters?,
        joinAllInstances: Boolean?,
    ): Single<CreateResult>

    abstract fun createTaskWithParent(
        createParameters: CreateParameters,
        parentTaskKey: TaskKey,
        dialogResult: DialogResult,
    ): Single<CreateResult>

    abstract fun createTaskWithoutReminder(
        createParameters: CreateParameters,
        projectKey: ProjectKey<*>?,
    ): Single<CreateResult>

    fun saveState() = parentScheduleManager.saveState()

    class CreateParameters(
        val name: String,
        val note: String? = null,
        private val imagePath: Pair<String, Uri>? = null,
    ) {

        init {
            check(name.isNotEmpty())
        }

        fun getImage(domainFactory: DomainFactory): Image? {
            if (imagePath == null) return null

            val uuid = newUuid()
            val json = TaskJson.Image(uuid, domainFactory.uuid)

            return Image(domainFactory, imagePath, uuid, json)
        }

        class Image(
            private val domainFactory: DomainFactory,
            private val path: Pair<String, Uri>,
            val uuid: String,
            val json: TaskJson.Image,
        ) {

            fun upload(taskKey: TaskKey) = Uploader.addUpload(domainFactory.deviceDbInfo, taskKey, uuid, path)
        }
    }

    class ProjectParameters(val key: ProjectKey<*>, val assignedTo: Set<UserKey>)

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

    enum class ShowDialog {

        JOIN, ADD, NONE
    }

    sealed class DialogResult {

        abstract val joinAllInstances: Boolean?
        abstract val addToAllInstances: Boolean?

        abstract fun matchesShowDialog(showDialog: ShowDialog): Boolean

        object None : DialogResult() {

            override val joinAllInstances: Boolean? = null
            override val addToAllInstances: Boolean? = null

            override fun matchesShowDialog(showDialog: ShowDialog) = true
        }

        data class JoinAllInstances(override val joinAllInstances: Boolean) : DialogResult() {

            override val addToAllInstances: Boolean get() = throw IllegalArgumentException()

            override fun matchesShowDialog(showDialog: ShowDialog) = showDialog == ShowDialog.JOIN
        }

        data class AddToAllInstances(override val addToAllInstances: Boolean) : DialogResult() {

            override val joinAllInstances: Boolean get() = throw IllegalArgumentException()

            override fun matchesShowDialog(showDialog: ShowDialog) = showDialog == ShowDialog.ADD
        }
    }
}