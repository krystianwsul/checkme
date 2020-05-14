package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.Observable

interface ParentScheduleManager {

    var parent: EditViewModel.ParentTreeData?
    val parentObservable: Observable<NullableWrapper<EditViewModel.ParentTreeData>>

    val schedules: List<ScheduleEntry>
    val scheduleObservable: Observable<List<ScheduleEntry>>

    val changed: Boolean

    fun setParentTask(taskKey: TaskKey)

    fun addSchedule(scheduleEntry: ScheduleEntry)

    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry)

    fun removeSchedule(position: Int)

    fun saveState(outState: Bundle)
}