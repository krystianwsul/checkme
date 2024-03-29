package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.krystianwsul.checkme.utils.NonNullRelayProperty
import com.krystianwsul.checkme.utils.NullableRelayProperty
import com.krystianwsul.common.utils.UserKey

class ParentMultiScheduleManager(
    savedInstanceState: Bundle?,
    defaultInitialParentScheduleState: ParentScheduleState,
    initialParent: ParentScheduleManager.Parent?,
    private val callbacks: ParentScheduleManager.Callbacks,
) : ParentScheduleManager {

    companion object {

        private const val KEY_INITIAL_STATE = "initialState"
        private const val KEY_STATE = "state"
    }

    private val initialState = savedInstanceState?.getParcelable(KEY_INITIAL_STATE) ?: defaultInitialParentScheduleState
    private val state = savedInstanceState?.getParcelable(KEY_STATE) ?: initialState.copy()

    private val parentProperty = NullableRelayProperty(initialParent) {
        callbacks.storeParent(it?.parentKey)

        if (it?.compatibleWithSchedule == false) {
            mutateSchedules { it.clear() }
        }

        assignedTo = setOf()
    }

    override var parent by parentProperty
        private set

    override val parentObservable = parentProperty.observable

    private val scheduleProperty = NonNullRelayProperty(state.schedules) {
        if (it.isNotEmpty() && parent?.compatibleWithSchedule == false) clearParent()

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

    override fun setNewParent(newParent: ParentScheduleManager.Parent?) {
        val clearParentTaskData = parent?.clearParentTaskData

        parent = newParent

        clearParentTaskData?.takeIf { newParent?.compatibleWithSchedule != false }?.let {
            schedules = it.second.map(::ScheduleEntry)

            if (newParent == clearParentTaskData.first) assignedTo = it.third
        }
    }

    override fun clearParent() {
        parent = parent!!.clearParentTaskData?.first
    }

    override fun clearParentAndReplaceSchedules() {
        val clearParentTaskData = parent!!.clearParentTaskData

        // definitely want to set this to null
        parent = clearParentTaskData?.first

        // mess with schedules only if data present
        clearParentTaskData?.let {
            schedules = it.second.map(::ScheduleEntry)
            assignedTo = it.third
        }
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

    private fun toState() = ParentScheduleState(schedules, assignedTo)

    override fun saveState() = Bundle().apply {
        putParcelable(KEY_STATE, toState())
        putParcelable(KEY_INITIAL_STATE, initialState)
    }
}