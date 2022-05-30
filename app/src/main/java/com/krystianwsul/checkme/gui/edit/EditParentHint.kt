package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import kotlinx.parcelize.Parcelize

sealed class EditParentHint : Parcelable {

    companion object {

        protected fun ProjectKey<*>.toCurrentParent() =
            EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Project(this))

        protected fun ProjectKey<*>.toParentKey() = EditViewModel.ParentKey.Project(this)
    }

    open val showInitialSchedule = true

    open val instanceKey: InstanceKey? = null

    abstract fun toCurrentParent(): EditViewModel.CurrentParentSource
    abstract fun toParentKey(): EditViewModel.ParentKey?

    open fun getReplacementHintForNewTask(taskKey: TaskKey): Instance? = null

    @Parcelize
    class Schedule(val date: Date, val timePair: TimePair, private val projectKey: ProjectKey.Shared? = null) :
        EditParentHint() {

        constructor(
            date: Date,
            pair: Pair<Date, HourMinute> = HourMinute.getNextHour(date),
        ) : this(pair.first, TimePair(pair.second), null)

        override fun toCurrentParent() = projectKey?.toCurrentParent() ?: EditViewModel.CurrentParentSource.None

        override fun toParentKey() = projectKey?.toParentKey()
    }

    @Parcelize
    class Task(private val taskKey: TaskKey) : EditParentHint() {

        override val showInitialSchedule get() = false

        override fun toCurrentParent() =
            EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Task(taskKey))

        override fun toParentKey() = EditViewModel.ParentKey.Task(taskKey)
    }

    @Parcelize
    class Instance(override val instanceKey: InstanceKey) : EditParentHint() {

        override val showInitialSchedule get() = false

        override fun toCurrentParent() =
            EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Task(instanceKey.taskKey))

        override fun toParentKey() = EditViewModel.ParentKey.Task(instanceKey.taskKey)

        override fun getReplacementHintForNewTask(taskKey: TaskKey) = this.takeIf { taskKey == instanceKey.taskKey }
    }

    @Parcelize
    class Project(val projectKey: ProjectKey<*>, override val showInitialSchedule: Boolean = true) : EditParentHint() {

        override fun toCurrentParent() = projectKey.toCurrentParent()

        override fun toParentKey() = projectKey.toParentKey()
    }
}