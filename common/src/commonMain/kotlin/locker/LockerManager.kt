package com.krystianwsul.common.locker

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

object LockerManager {

    var state: State = State.None
        private set

    sealed class State {

        object None : State()

        class Now(val now: ExactTimeStamp) : State()

        class Locker(val now: ExactTimeStamp) : State() {

            val privateProjectLocker by lazy { ProjectLocker<ProjectType.Private>(this) }

            private val sharedProjectLockers = mutableMapOf<ProjectKey.Shared, ProjectLocker<ProjectType.Shared>>()

            fun getSharedProjectLocker(projectKey: ProjectKey.Shared): ProjectLocker<ProjectType.Shared> {
                if (!sharedProjectLockers.containsKey(projectKey))
                    sharedProjectLockers[projectKey] = ProjectLocker(this)

                return sharedProjectLockers.getValue(projectKey)
            }
        }
    }

    fun <T : Any> setLocker(action: (ExactTimeStamp) -> T): T {
        check(state == State.None)

        val now = ExactTimeStamp.now

        state = State.Locker(now)
        val ret = action(now)
        state = State.None

        return ret
    }
}