package com.krystianwsul.checkme.gui.tasks.create

import android.content.Intent
import android.os.Parcelable
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import kotlinx.android.parcel.Parcelize

sealed class CreateTaskParameters : Parcelable {

    companion object {

        private const val KEY_SHORTCUT_ID = "android.intent.extra.shortcut.ID"

        fun fromIntent(intent: Intent): CreateTaskParameters {
            return when {
                intent.hasExtra(CreateTaskActivity.KEY_PARAMETERS) -> {
                    check(intent.action != Intent.ACTION_SEND)
                    check(!intent.hasExtra(KEY_SHORTCUT_ID))
                    check(!intent.hasExtra(CreateTaskActivity.KEY_PARENT_PROJECT_KEY))

                    intent.getParcelableExtra(CreateTaskActivity.KEY_PARAMETERS)!!
                }
                intent.action == Intent.ACTION_SEND -> {
                    check(!intent.hasExtra(CreateTaskActivity.KEY_PARENT_PROJECT_KEY))

                    check(intent.type == "text/plain")

                    val nameHint = intent.getStringExtra(Intent.EXTRA_TEXT)
                    check(!nameHint.isNullOrEmpty())

                    val taskKey = if (intent.hasExtra(KEY_SHORTCUT_ID)) {
                        TaskKey.fromShortcut(intent.getStringExtra(KEY_SHORTCUT_ID)!!)
                    } else {
                        null
                    }

                    Share(nameHint, taskKey)
                }
                intent.hasExtra(KEY_SHORTCUT_ID) -> {
                    check(!intent.hasExtra(CreateTaskActivity.KEY_PARENT_PROJECT_KEY))

                    Shortcut(TaskKey.fromShortcut(intent.getStringExtra(KEY_SHORTCUT_ID)!!))
                }
                intent.hasExtra(CreateTaskActivity.KEY_PARENT_PROJECT_KEY) -> {
                    check(intent.hasExtra(CreateTaskActivity.KEY_PARENT_TASK))
                    check(intent.hasExtra(CreateTaskActivity.KEY_PARENT_PROJECT_TYPE))

                    val projectKey = ProjectKey.Type
                            .values()[intent.getIntExtra(CreateTaskActivity.KEY_PARENT_PROJECT_TYPE, -1)]
                            .newKey(intent.getStringExtra(CreateTaskActivity.KEY_PARENT_PROJECT_KEY)!!)

                    Shortcut(TaskKey(projectKey, intent.getStringExtra(CreateTaskActivity.KEY_PARENT_TASK)!!))
                }
                else -> None
            }
        }
    }

    abstract fun startViewModel(viewModel: CreateTaskViewModel)

    @Parcelize
    class Create(
            val hint: CreateTaskActivity.Hint? = null,
            val parentScheduleState: CreateTaskActivity.ParentScheduleState? = null,
            val nameHint: String? = null
    ) : CreateTaskParameters() {

        override fun startViewModel(viewModel: CreateTaskViewModel) =
                viewModel.start(parentTaskKeyHint = (hint as? CreateTaskActivity.Hint.Task)?.taskKey)
    }

    @Parcelize
    class Join(
            val taskKeys: List<TaskKey>,
            val hint: CreateTaskActivity.Hint? = null,
            val removeInstanceKeys: List<InstanceKey> = listOf()
    ) : CreateTaskParameters() {

        init {
            check(taskKeys.size > 1)
        }

        override fun startViewModel(viewModel: CreateTaskViewModel) =
                viewModel.start(null, taskKeys, (hint as? CreateTaskActivity.Hint.Task)?.taskKey)
    }

    @Parcelize
    class Copy(val taskKey: TaskKey) : CreateTaskParameters() {

        override fun startViewModel(viewModel: CreateTaskViewModel) = viewModel.start(taskKey)
    }

    @Parcelize
    class Edit(val taskKey: TaskKey) : CreateTaskParameters() {

        override fun startViewModel(viewModel: CreateTaskViewModel) = viewModel.start(taskKey)
    }

    @Parcelize
    class Shortcut(val parentTaskKeyHint: TaskKey) : CreateTaskParameters() {

        override fun startViewModel(viewModel: CreateTaskViewModel) =
                viewModel.start(parentTaskKeyHint = parentTaskKeyHint)
    }

    @Parcelize
    class Share(val nameHint: String, val parentTaskKeyHint: TaskKey?) : CreateTaskParameters() {

        override fun startViewModel(viewModel: CreateTaskViewModel) =
                viewModel.start(parentTaskKeyHint = parentTaskKeyHint)
    }

    @Parcelize
    object None : CreateTaskParameters() {

        override fun startViewModel(viewModel: CreateTaskViewModel) = viewModel.start()
    }
}