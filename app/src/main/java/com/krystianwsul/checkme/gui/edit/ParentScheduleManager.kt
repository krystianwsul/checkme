package com.krystianwsul.checkme.gui.edit

import com.krystianwsul.checkme.utils.NonNullRelayProperty
import com.krystianwsul.checkme.utils.NullableRelayProperty
import com.krystianwsul.checkme.viewmodels.EditViewModel

class ParentScheduleManager(
        state: ParentScheduleState,
        initialParent: EditViewModel.ParentTreeData?
) {

    private val parentProperty = NullableRelayProperty(initialParent) {
        if (it?.parentKey is EditViewModel.ParentKey.Task)
            mutateSchedules { it.clear() }
    }

    var parent by parentProperty
    val parentObservable = parentProperty.observable

    private val scheduleProperty = NonNullRelayProperty(state.schedules) {
        if (it.isNotEmpty() && parent?.parentKey is EditViewModel.ParentKey.Task)
            parent = null
    }

    var schedules by scheduleProperty
    val scheduleObservable = scheduleProperty.observable

    private fun mutateSchedules(action: (MutableList<ScheduleEntry>) -> Unit): Unit =
            scheduleProperty.mutate { it.toMutableList().also(action) }

    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry) =
            mutateSchedules { it[position] = scheduleEntry }

    fun removeSchedule(position: Int) = mutateSchedules { it.removeAt(position) }

    fun addSchedule(scheduleEntry: ScheduleEntry) {
        mutateSchedules { it += scheduleEntry }
    }

    fun toState() = ParentScheduleState(parent?.parentKey, schedules)
}