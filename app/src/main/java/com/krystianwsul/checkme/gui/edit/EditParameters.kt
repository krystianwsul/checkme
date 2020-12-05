package com.krystianwsul.checkme.gui.edit

import android.content.Intent
import android.os.Parcelable
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import kotlinx.parcelize.Parcelize

sealed class EditParameters : Parcelable {

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
                    check(!intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_KEY))

                    Shortcut(TaskKey.fromShortcut(intent.getStringExtra(KEY_SHORTCUT_ID)!!))
                }
                intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_KEY) -> {
                    check(intent.hasExtra(EditActivity.KEY_PARENT_TASK))
                    check(intent.hasExtra(EditActivity.KEY_PARENT_PROJECT_TYPE))

                    val projectKey = ProjectKey.Type
                            .values()[intent.getIntExtra(EditActivity.KEY_PARENT_PROJECT_TYPE, -1)]
                            .newKey(intent.getStringExtra(EditActivity.KEY_PARENT_PROJECT_KEY)!!)

                    Shortcut(TaskKey(projectKey, intent.getStringExtra(EditActivity.KEY_PARENT_TASK)!!))
                }
                else -> None
            }
        }
    }

    abstract fun startViewModel(viewModel: EditViewModel)

    @Parcelize
    class Create(
            val hint: EditActivity.Hint? = null,
            val parentScheduleState: ParentScheduleState? = null,
            val nameHint: String? = null,
            val showFirstSchedule: Boolean = true
    ) : EditParameters() {

        override fun startViewModel(viewModel: EditViewModel) =
                viewModel.start(parentTaskKeyHint = (hint as? EditActivity.Hint.Task)?.taskKey)
    }

    @Parcelize
    class Join(
            val taskKeys: List<TaskKey>,
            val hint: EditActivity.Hint? = null,
            val removeInstanceKeys: List<InstanceKey> = listOf()
    ) : EditParameters() {

        init {
            check(taskKeys.size > 1)
        }

        override fun startViewModel(viewModel: EditViewModel) = viewModel.start(
                EditViewModel.StartParameters.Join(taskKeys),
                (hint as? EditActivity.Hint.Task)?.taskKey
        )
    }

    @Parcelize
    class Copy(val taskKey: TaskKey) : EditParameters() {

        override fun startViewModel(viewModel: EditViewModel) = viewModel.start(taskKey)
    }

    @Parcelize
    class Edit(val taskKey: TaskKey, val openedFromInstanceKey: InstanceKey? = null) : EditParameters() {

        constructor(instanceKey: InstanceKey) : this(instanceKey.taskKey, instanceKey)

        override fun startViewModel(viewModel: EditViewModel) = viewModel.start(taskKey)
    }

    @Parcelize
    class Shortcut(val parentTaskKeyHint: TaskKey) : EditParameters() {

        override fun startViewModel(viewModel: EditViewModel) =
                viewModel.start(parentTaskKeyHint = parentTaskKeyHint)
    }

    @Parcelize
    class Share(val nameHint: String, val parentTaskKeyHint: TaskKey?) : EditParameters() {

        override fun startViewModel(viewModel: EditViewModel) =
                viewModel.start(parentTaskKeyHint = parentTaskKeyHint)
    }

    @Parcelize
    object None : EditParameters() {

        override fun startViewModel(viewModel: EditViewModel) = viewModel.start()
    }
}