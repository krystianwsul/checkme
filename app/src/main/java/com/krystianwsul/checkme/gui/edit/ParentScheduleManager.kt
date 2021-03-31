package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable

interface ParentScheduleManager {

    var parent: Parent?
    val parentObservable: Observable<NullableWrapper<Parent>>

    val schedules: List<ScheduleEntry>
    val scheduleObservable: Observable<List<ScheduleEntry>>

    var assignedTo: Set<UserKey>
    val assignedToObservable: Observable<Set<UserKey>>

    val assignedToUsers: Map<UserKey, EditViewModel.UserData>

    val changed: Boolean

    fun addSchedule(scheduleEntry: ScheduleEntry)

    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry)

    fun removeSchedule(schedulePosition: Int)

    fun saveState(): Bundle

    fun removeAssignedTo(userKey: UserKey)

    interface Callbacks {

        fun getInitialParent(): EditViewModel.ParentTreeData?

        fun storeParent(parentKey: EditViewModel.ParentKey?)
    }

    interface Parent {

        val name: String
        val parentKey: EditViewModel.ParentKey
        val projectUsers: Map<UserKey, EditViewModel.UserData>
    }
}