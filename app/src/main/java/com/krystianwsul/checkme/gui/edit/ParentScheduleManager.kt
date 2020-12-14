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

    val assignedToUsers: Map<UserKey, EditViewModel.UserData>

    val changed: Boolean

    fun trySetParentTask(taskKey: TaskKey): Boolean

    fun addSchedule(scheduleEntry: ScheduleEntry)

    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry)

    fun removeSchedule(schedulePosition: Int)

    fun saveState(outState: Bundle)

    fun removeAssignedTo(userKey: UserKey)
}