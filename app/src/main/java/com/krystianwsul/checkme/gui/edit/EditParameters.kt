package com.krystianwsul.checkme.gui.edit

import android.content.Intent
import android.net.Uri
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

    open val startParameters: EditViewModel.StartParameters = EditViewModel.StartParameters.Create
    open val parentTaskKeyHint: TaskKey? = null

    @Parcelize
    class Create(
            val hint: EditActivity.Hint? = null,
            val parentScheduleState: ParentScheduleState? = null,
            val nameHint: String? = null,
            val showFirstSchedule: Boolean = true,
    ) : EditParameters() {

        override val parentTaskKeyHint get() = (hint as? EditActivity.Hint.Task)?.taskKey
    }

    @Parcelize
    class Join(val joinables: List<Joinable>, val hint: EditActivity.Hint? = null) : EditParameters() {

        override val startParameters get() = EditViewModel.StartParameters.Join(joinables)
        override val parentTaskKeyHint get() = (hint as? EditActivity.Hint.Task)?.taskKey

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

    @Parcelize
    class Copy(val taskKey: TaskKey) : EditParameters() {

        override val startParameters get() = EditViewModel.StartParameters.Task(taskKey)
    }

    @Parcelize
    class Edit(val taskKey: TaskKey, val openedFromInstanceKey: InstanceKey? = null) : EditParameters() {

        constructor(instanceKey: InstanceKey) : this(instanceKey.taskKey, instanceKey)

        override val startParameters get() = EditViewModel.StartParameters.Task(taskKey)
    }

    @Parcelize
    class Shortcut(override val parentTaskKeyHint: TaskKey) : EditParameters()

    @Parcelize
    class Share private constructor(
            val nameHint: String? = null,
            override val parentTaskKeyHint: TaskKey? = null,
            val uri: Uri? = null,
    ) : EditParameters() {

        companion object {

            fun fromText(nameHint: String, parentTaskKeyHint: TaskKey?) = Share(nameHint, parentTaskKeyHint)

            fun fromUri(uri: Uri) = Share(uri = uri)
        }
    }

    @Parcelize
    object None : EditParameters()
}