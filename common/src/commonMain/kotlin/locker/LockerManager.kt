package com.krystianwsul.common.locker

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey

object LockerManager {

    var state: State = State.None
        private set

    fun <T : ProjectType> getTaskLocker(taskKey: TaskKey): TaskLocker<T>? {
        val state = state as? State.Locker ?: return null

        @Suppress("UNCHECKED_CAST")
        return when (taskKey.projectKey) {
            is ProjectKey.Private -> state.privateProjectLocker
            is ProjectKey.Shared -> state.getSharedProjectLocker(taskKey.projectKey)
        }.getTaskLocker(taskKey) as TaskLocker<T>
    }

    fun <T : ProjectType> getInstanceLocker(instanceKey: InstanceKey) = getTaskLocker<T>(instanceKey.taskKey)?.getInstanceLocker(instanceKey)

    sealed class State {

        object None : State()

        class Now(val now: ExactTimeStamp.Local) : State()

        class Locker(val now: ExactTimeStamp.Local) : State() {

            val privateProjectLocker by lazy { ProjectLocker<ProjectType.Private>(this) }

            private val sharedProjectLockers = mutableMapOf<ProjectKey.Shared, ProjectLocker<ProjectType.Shared>>()

            fun getSharedProjectLocker(projectKey: ProjectKey.Shared): ProjectLocker<ProjectType.Shared> {
                if (!sharedProjectLockers.containsKey(projectKey))
                    sharedProjectLockers[projectKey] = ProjectLocker(this)

                return sharedProjectLockers.getValue(projectKey)
            }
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