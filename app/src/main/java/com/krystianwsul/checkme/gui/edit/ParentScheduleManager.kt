package com.krystianwsul.checkme.gui.edit

import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable

interface ParentScheduleManager {

    var parent: EditViewModel.ParentTreeData?
    val parentObservable: Observable<NullableWrapper<EditViewModel.ParentTreeData>>

    val schedules: List<ScheduleEntry>
    val scheduleObservable: Observable<List<ScheduleEntry>> // todo group expose list of adapter items in delegate

    fun addSchedule(scheduleEntry: ScheduleEntry)

    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry)

    fun removeSchedule(position: Int)

    fun toState(): ParentScheduleState // todo group task encapsulate, move state class into multi
}