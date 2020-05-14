package com.krystianwsul.checkme.gui.edit

import com.krystianwsul.checkme.utils.NonNullRelayProperty
import com.krystianwsul.checkme.utils.NullableRelayProperty
import com.krystianwsul.checkme.viewmodels.EditViewModel

class ParentMultiScheduleManager(
        state: ParentScheduleState,
        initialParent: EditViewModel.ParentTreeData?
) : ParentScheduleManager {

    private val parentProperty = NullableRelayProperty(initialParent) {
        if (it?.parentKey is EditViewModel.ParentKey.Task)
            mutateSchedules { it.clear() }
    }

    override var parent by parentProperty
    override val parentObservable = parentProperty.observable

    private val scheduleProperty = NonNullRelayProperty(state.schedules) {
        if (it.isNotEmpty() && parent?.parentKey is EditViewModel.ParentKey.Task)
            parent = null
    }

    override var schedules by scheduleProperty
        private set

    override val scheduleObservable = scheduleProperty.observable

    private fun mutateSchedules(action: (MutableList<ScheduleEntry>) -> Unit): Unit =
            scheduleProperty.mutate { it.toMutableList().also(action) }

    override fun setSchedule(position: Int, scheduleEntry: ScheduleEntry) =
            mutateSchedules { it[position] = scheduleEntry }

    override fun removeSchedule(position: Int) = mutateSchedules { it.removeAt(position) }

    override fun addSchedule(scheduleEntry: ScheduleEntry) {
        mutateSchedules { it += scheduleEntry }
    }

    override fun toState() = ParentScheduleState(parent?.parentKey, schedules)
}