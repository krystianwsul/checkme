package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.utils.NonNullRelayProperty
import com.krystianwsul.checkme.utils.NullableRelayProperty
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey

class ParentMultiScheduleManager(
        savedInstanceState: Bundle?,
        initialStateGetter: () -> ParentScheduleState,
        private val parentLookup: EditDelegate.ParentLookup
) : ParentScheduleManager {

    companion object {

        private const val KEY_INITIAL_STATE = "initialState"
        private const val KEY_STATE = "state"
    }

    private val initialState = savedInstanceState?.getParcelable(KEY_INITIAL_STATE)
            ?: initialStateGetter()

    private val state = savedInstanceState?.getParcelable(KEY_STATE) ?: initialState.copy()

    private val parentProperty = NullableRelayProperty(state.parentKey?.let { parentLookup.findTaskData(it) }) {
        if (it?.parentKey is EditViewModel.ParentKey.Task) mutateSchedules { it.clear() }

        assignedTo = setOf()
    }

    override var parent by parentProperty
    override val parentObservable = parentProperty.observable

    private val scheduleProperty = NonNullRelayProperty(state.schedules) {
        if (it.isNotEmpty() && parent?.parentKey is EditViewModel.ParentKey.Task)
            parent = null

        if (it.isEmpty()) assignedTo = setOf()
    }

    override var schedules by scheduleProperty
        private set

    override val scheduleObservable = scheduleProperty.observable

    private val assignedToProperty = NonNullRelayProperty(state.assignedTo)
    override var assignedTo by assignedToProperty
    override val assignedToObservable = assignedToProperty.observable
    override val assignedToUsers get() = assignedTo.associateWith { parent!!.projectUsers.getValue(it) }

    override val changed get() = toState() != initialState

    override fun trySetParentTask(taskKey: TaskKey): Boolean {
        parent = parentLookup.tryFindTaskData(EditViewModel.ParentKey.Task(taskKey)) ?: return false

        return true
    }

    private fun mutateSchedules(action: (MutableList<ScheduleEntry>) -> Unit): Unit =
            scheduleProperty.mutate { it.toMutableList().also(action) }

    override fun setSchedule(position: Int, scheduleEntry: ScheduleEntry) =
            mutateSchedules { it[position] = scheduleEntry }

    override fun removeSchedule(schedulePosition: Int) = mutateSchedules { it.removeAt(schedulePosition) }

    override fun addSchedule(scheduleEntry: ScheduleEntry) = mutateSchedules { it += scheduleEntry }

    override fun removeAssignedTo(userKey: UserKey) {
        assignedTo -= userKey
    }

    private fun toState() = ParentScheduleState(parent?.parentKey, schedules, assignedTo)

    override fun saveState(outState: Bundle) {
        outState.apply {
            putParcelable(KEY_STATE, toState())
            putParcelable(KEY_INITIAL_STATE, initialState)
        }
    }
}