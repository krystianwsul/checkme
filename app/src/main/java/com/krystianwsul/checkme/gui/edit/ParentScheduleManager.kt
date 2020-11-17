package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.Observable

interface ParentScheduleManager {

    var parent: EditViewModel.ParentTreeData?
    val parentObservable: Observable<NullableWrapper<EditViewModel.ParentTreeData>>

    val schedules: List<ScheduleEntry>
    val scheduleObservable: Observable<List<ScheduleEntry>>

    var assignedTo: Set<UserKey>
    val assignedToObservable: Observable<Set<UserKey>>

    val assignedToUsers: List<EditViewModel.UserData>

    val changed: Boolean

    fun setParentTask(taskKey: TaskKey)

    fun addSchedule(scheduleEntry: ScheduleEntry)

    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry)

    fun removeSchedule(position: Int)

    fun saveState(outState: Bundle)
}