package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import kotlinx.parcelize.Parcelize

sealed class EditParentHint : Parcelable {

    companion object {

        protected fun ProjectKey.Shared.toCurrentParent() =
            EditViewModel.CurrentParentSource.Set(EditViewModel.ParentKey.Project(this))

        protected fun ProjectKey.Shared.toParentKey() = EditViewModel.ParentKey.Project(this)
    }

    open val showInitialSchedule = true

    abstract fun toCurrentParent(): EditViewModel.CurrentParentSource
    abstract fun toParentKey(): EditViewModel.ParentKey?

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
    class Project(val projectKey: ProjectKey.Shared) : EditParentHint() {

        override fun toCurrentParent() = projectKey.toCurrentParent()

        override fun toParentKey() = projectKey.toParentKey()
    }
}