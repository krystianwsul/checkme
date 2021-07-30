package com.krystianwsul.common.locker

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey

object LockerManager {

    var state: State = State.None
        private set

    private fun getTaskLocker(taskKey: TaskKey): TaskLocker? {
        val state = state as? State.Locker ?: return null

        return state.taskLockers.getOrPut(taskKey) { TaskLocker(state) }
    }

    fun getInstanceLocker(instanceKey: InstanceKey) = getTaskLocker(instanceKey.taskKey)?.getInstanceLocker(instanceKey)

    sealed class State {

        object None : State()

        class Now(val now: ExactTimeStamp.Local) : State()

        class Locker(val now: ExactTimeStamp.Local) : State() {

            val taskLockers = mutableMapOf<TaskKey, TaskLocker>()
        }
    }

    fun <T : Any> setLocker(action: (ExactTimeStamp.Local) -> T): T {
        check(state == State.None)

        val now = ExactTimeStamp.Local.now

        state = State.Locker(now)
        val ret = action(now)
        state = State.None

        return ret
    }
}