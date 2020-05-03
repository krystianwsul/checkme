package com.krystianwsul.checkme.gui.tasks.create

import android.content.Intent
import android.os.Parcelable
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import kotlinx.android.parcel.Parcelize

sealed class Parameters : Parcelable {

    companion object {

        private const val KEY_SHORTCUT_ID = "android.intent.extra.shortcut.ID"

        fun fromIntent(intent: Intent): Parameters {
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

    open val taskKeys: List<TaskKey>? = null
    open val removeInstanceKeys: List<InstanceKey> = listOf()
    open val copy = false
    open val nameHint: String? = null
    open val taskKey: TaskKey? = null
    open val hint: CreateTaskActivity.Hint? = null
    open val parentScheduleState: CreateTaskActivity.ParentScheduleState? = null

    @Parcelize
    class Create(
            override val hint: CreateTaskActivity.Hint? = null,
            override val parentScheduleState: CreateTaskActivity.ParentScheduleState? = null,
            override val nameHint: String? = null
    ) : Parameters()

    @Parcelize
    class Join(
            override val taskKeys: List<TaskKey>,
            override val hint: CreateTaskActivity.Hint? = null,
            override val removeInstanceKeys: List<InstanceKey> = listOf()
    ) : Parameters() {

        init {
            check(taskKeys.size > 1)
        }
    }

    @Parcelize
    class Copy(override val taskKey: TaskKey) : Parameters() {

        override val copy get() = true
    }

    @Parcelize
    class Edit(override val taskKey: TaskKey) : Parameters()

    @Parcelize
    class Shortcut(private val parentTaskKeyHint: TaskKey) : Parameters() {

        override val hint get() = CreateTaskActivity.Hint.Task(parentTaskKeyHint)
    }

    @Parcelize
    class Share(override val nameHint: String, private val parentTaskKeyHint: TaskKey?) : Parameters() {

        override val hint get() = parentTaskKeyHint?.let { CreateTaskActivity.Hint.Task(parentTaskKeyHint) }
    }

    @Parcelize
    object None : Parameters()
}