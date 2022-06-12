package com.krystianwsul.checkme.gui.edit

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream

sealed interface EditParameters : Parcelable {

    companion object {

        private const val KEY_SHORTCUT_ID = "android.intent.extra.shortcut.ID"

        fun fromIntent(intent: Intent): EditParameters {
            return when {
                intent.hasExtra(EditActivity.KEY_PARAMETERS) -> {
                    check(intent.action != Intent.ACTION_SEND)
                    check(!intent.hasExtra(KEY_SHORTCUT_ID))
                    check(!intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_KEY))

                    intent.getParcelableExtra(EditActivity.KEY_PARAMETERS)!!
                }
                intent.action == Intent.ACTION_SEND -> {
                    check(!intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_KEY))

                    if (intent.type == "text/plain") {
                        val nameHint = intent.getStringExtra(Intent.EXTRA_TEXT)
                        check(!nameHint.isNullOrEmpty())

                        val taskKey = if (intent.hasExtra(KEY_SHORTCUT_ID)) {
                            TaskKey.fromShortcut(intent.getStringExtra(KEY_SHORTCUT_ID)!!)
                        } else {
                            null
                        }

                        Share.fromText(nameHint, taskKey)
                    } else {
                        intent.type!!.startsWith("image/")

                        Share.fromUri(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)
                    }
                }
                intent.hasExtra(KEY_SHORTCUT_ID) -> {
                    check(!intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_KEY))

                    Shortcut(TaskKey.fromShortcut(intent.getStringExtra(KEY_SHORTCUT_ID)!!))
                }
                intent.hasExtra(EditActivity.KEY_PARENT_TASK) -> {
                    val taskId = intent.getStringExtra(EditActivity.KEY_PARENT_TASK)!!

                    val taskKey = if (intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_KEY)) {
                        check(intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_TYPE))

                        val projectKey = ProjectKey.Type
                            .values()[intent.getIntExtra(EditActivity.KEY_PARENT_PROJECT_TYPE, -1)]
                            .newKey(intent.getStringExtra(EditActivity.KEY_PARENT_PROJECT_KEY)!!)

                        TaskKey.Project(projectKey, taskId)
                    } else {
                        TaskKey.Root(taskId)
                    }

                    Shortcut(taskKey)
                }
                else -> None
            }
        }

        protected fun EditParentHint.toScheduleHint() = this as? EditParentHint.Schedule
    }

    val startParameters: EditViewModel.StartParameters

    val scheduleParameters: EditViewModel.ScheduleParameters

    val currentParentSource: EditViewModel.CurrentParentSource

    // for parent instance picker
    val excludedTaskKeys: Set<TaskKey> get() = emptySet()

    fun getInitialEditImageState(savedEditImageState: EditImageState?) = savedEditImageState ?: EditImageState.None

    fun getInitialEditImageStateSingle(
        savedEditImageState: EditImageState?,
        taskDataSingle: Single<NullableWrapper<EditViewModel.TaskData>>,
        editActivity: EditActivity,
    ) = Single.just(getInitialEditImageState(savedEditImageState))

    fun getReplacementHintForNewTask(taskKey: TaskKey): EditParentHint.Instance? = null

    sealed interface CreateDelegateParameters : EditParameters {

        override val startParameters: EditViewModel.StartParameters get() = EditViewModel.StartParameters.Create(null)
    }

    @Parcelize
    class Create(
        val hint: EditParentHint? = null,
        private val parentScheduleState: ParentScheduleState? = null,
        val nameHint: String? = null,
        private val showFirstSchedule: Boolean = true,
    ) : CreateDelegateParameters {

        override val startParameters get() = EditViewModel.StartParameters.Create(hint?.instanceKey)

        override val scheduleParameters: EditViewModel.ScheduleParameters
            get() {
                val defaultScheduleOverride = hint?.toScheduleHint()?.dateTimePair

                return if (parentScheduleState != null) {
                    EditViewModel.ScheduleParameters.Override(defaultScheduleOverride, parentScheduleState)
                } else {
                    EditViewModel.ScheduleParameters.Normal(
                        defaultScheduleOverride,
                        hint?.showInitialSchedule != false && showFirstSchedule,
                    )
                }
            }

        override val currentParentSource get() = hint?.toCurrentParent() ?: EditViewModel.CurrentParentSource.None

        override fun getReplacementHintForNewTask(taskKey: TaskKey) = hint?.getReplacementHintForNewTask(taskKey)
    }

    @Parcelize
    class MigrateDescription(val taskKey: TaskKey) : CreateDelegateParameters {

        override val startParameters get() = EditViewModel.StartParameters.MigrateDescription(taskKey)

        override val scheduleParameters get() = EditViewModel.ScheduleParameters.Normal(null, false)

        override val currentParentSource get() = EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Task(taskKey))
    }

    @Parcelize
    class Share private constructor(
        val nameHint: String? = null,
        private val parentTaskKeyHint: TaskKey? = null,
        private val uri: Uri? = null,
    ) : CreateDelegateParameters {

        companion object {

            fun fromText(nameHint: String, parentTaskKeyHint: TaskKey?) = Share(nameHint, parentTaskKeyHint)

            fun fromUri(uri: Uri) = Share(uri = uri)
        }

        override val currentParentSource
            get() = parentTaskKeyHint?.let {
                EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Task(it))
            } ?: EditViewModel.CurrentParentSource.None

        override val scheduleParameters get() = EditViewModel.ScheduleParameters.Normal(null, parentTaskKeyHint == null)

        override fun getInitialEditImageStateSingle(
            savedEditImageState: EditImageState?,
            taskDataSingle: Single<NullableWrapper<EditViewModel.TaskData>>,
            editActivity: EditActivity,
        ): Single<EditImageState> {
            return when {
                savedEditImageState?.dontOverwrite == true -> Single.just(savedEditImageState)
                uri != null -> {
                    if (uri.scheme == "file") {
                        uri.toString().let { Single.just(EditImageState.Selected(it, it)) }
                    } else {
                        check(uri.scheme == "content")

                        copyFile(editActivity)
                    }
                }
                else -> super.getInitialEditImageStateSingle(savedEditImageState, taskDataSingle, editActivity)
            }
        }

        private fun copyFile(editActivity: EditActivity): Single<EditImageState> {
            return Single.fromCallable<EditImageState> {
                MyApplication.instance
                    .getRxPaparazzoDir()
                    .mkdirs()

                val outputFile = File.createTempFile(
                    "copiedImage",
                    null,
                    MyApplication.instance.getRxPaparazzoDir(),
                )

                editActivity.contentResolver
                    .openInputStream(uri!!)
                    .use { inputStream ->
                        FileOutputStream(outputFile).use { outputStream ->
                            IOUtils.copy(inputStream, outputStream)
                        }
                    }

                EditImageState.Selected(outputFile)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }

    @Parcelize
    class Shortcut(private val parentTaskKeyHint: TaskKey) : CreateDelegateParameters {

        override val scheduleParameters get() = EditViewModel.ScheduleParameters.Normal(null, false)

        override val currentParentSource
            get() = EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Task(parentTaskKeyHint))
    }

    @Parcelize
    object None : CreateDelegateParameters {

        override val scheduleParameters get() = EditViewModel.ScheduleParameters.Normal(null, true)

        override val currentParentSource get() = EditViewModel.CurrentParentSource.None
    }

    @Parcelize
    class Join(val joinables: List<Joinable>, private val hint: EditParentHint? = null) : EditParameters {

        override val startParameters get() = EditViewModel.StartParameters.Join(joinables)

        override val scheduleParameters
            get() = EditViewModel.ScheduleParameters.Normal(
                hint?.toScheduleHint()?.dateTimePair,
                hint?.showInitialSchedule != false,
            )

        override val currentParentSource
            get() = hint?.toCurrentParent()
                .takeIf { it !is EditViewModel.CurrentParentSource.None }
                ?: EditViewModel.CurrentParentSource.FromTasks(joinables.map { it.taskKey }.toSet())

        override val excludedTaskKeys get() = joinables.map { it.taskKey }.toSet()

        init {
            check(joinables.size > 1)
        }

        sealed class Joinable : Parcelable {

            abstract val taskKey: TaskKey

            abstract val instanceKey: InstanceKey?

            @Parcelize
            data class Task(override val taskKey: TaskKey) : Joinable() {

                override val instanceKey: InstanceKey? get() = null
            }

            @Parcelize
            data class Instance(override val instanceKey: InstanceKey) : Joinable() {

                override val taskKey: TaskKey get() = instanceKey.taskKey
            }
        }
    }

    sealed interface Existing : EditParameters {

        override val scheduleParameters get() = EditViewModel.ScheduleParameters.FromTaskData
    }

    @Parcelize
    class Copy(val copySource: CopySource) : Existing {

        constructor(taskKey: TaskKey) : this(CopySource.Task(taskKey))

        constructor(instanceKey: InstanceKey) : this(CopySource.Instance(instanceKey))

        override val startParameters get() = EditViewModel.StartParameters.TaskOrInstance(copySource)

        override val currentParentSource
            get() = when (copySource) {
                is CopySource.Task -> EditViewModel.CurrentParentSource.FromTask(copySource.taskKey)
                is CopySource.Instance -> EditViewModel.CurrentParentSource.FromInstance(copySource.instanceKey)
            }

        sealed class CopySource : Parcelable {

            abstract val taskKey: TaskKey

            @Parcelize
            data class Task(override val taskKey: TaskKey) : CopySource()

            @Parcelize
            data class Instance(val instanceKey: InstanceKey) : CopySource() {

                override val taskKey get() = instanceKey.taskKey
            }
        }
    }

    @Parcelize
    class Edit(val taskKey: TaskKey, val openedFromInstanceKey: InstanceKey? = null) : Existing {

        constructor(instanceKey: InstanceKey) : this(instanceKey.taskKey, instanceKey)

        override val startParameters get() = EditViewModel.StartParameters.TaskOrInstance(Copy.CopySource.Task(taskKey))

        override val currentParentSource get() = EditViewModel.CurrentParentSource.FromTask(taskKey)

        override val excludedTaskKeys get() = setOf(taskKey)

        override fun getInitialEditImageStateSingle(
            savedEditImageState: EditImageState?,
            taskDataSingle: Single<NullableWrapper<EditViewModel.TaskData>>,
            editActivity: EditActivity,
        ): Single<EditImageState> {
            return if (savedEditImageState?.dontOverwrite == true) {
                Single.just(savedEditImageState)
            } else {
                taskDataSingle.map {
                    it.value!!
                        .imageState
                        ?.let(EditImageState::Existing)
                        ?: getInitialEditImageState(savedEditImageState)
                }
            }
        }
    }
}