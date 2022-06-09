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

        protected fun TaskKey.toParentKey() = EditViewModel.ParentKey.Task(this)
        protected fun ProjectKey<*>.toParentKey() = EditViewModel.ParentKey.Project(this)

        protected fun EditViewModel.ParentKey.toCurrentParent() = EditViewModel.CurrentParentSource.Set(this)

        protected fun TaskKey.toCurrentParent() = toParentKey().toCurrentParent()
        protected fun ProjectKey<*>.toCurrentParent() = toParentKey().toCurrentParent()
    }

    open val showInitialSchedule = true

    open val instanceKey: InstanceKey? = null

    abstract fun toCurrentParent(): EditViewModel.CurrentParentSource

    open fun getReplacementHintForNewTask(taskKey: TaskKey): Instance? = null

    @Parcelize
    class Schedule(val date: Date, val timePair: TimePair, private val projectKey: ProjectKey.Shared? = null) :
        EditParentHint() {

        constructor(
            date: Date,
            pair: Pair<Date, HourMinute> = HourMinute.getNextHour(date),
        ) : this(pair.first, TimePair(pair.second), null)

        override fun toCurrentParent() = projectKey?.toCurrentParent() ?: EditViewModel.CurrentParentSource.None
    }

    @Parcelize
    class Task(private val taskKey: TaskKey) : EditParentHint() {

        override val showInitialSchedule get() = false

        override fun toCurrentParent() = taskKey.toCurrentParent()
    }

    @Parcelize
    class Instance(override val instanceKey: InstanceKey, val projectKey: ProjectKey.Shared?) : EditParentHint() {

        override val showInitialSchedule get() = false

        override fun toCurrentParent() = projectKey?.toCurrentParent() ?: instanceKey.taskKey.toCurrentParent()

        override fun getReplacementHintForNewTask(taskKey: TaskKey) = this.takeIf { taskKey == instanceKey.taskKey }
    }

    @Parcelize
    class Project(val projectKey: ProjectKey<*>, override val showInitialSchedule: Boolean = true) : EditParentHint() {

        override fun toCurrentParent() = projectKey.toCurrentParent()
    }
}