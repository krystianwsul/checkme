package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.dependencies.RootTaskKeyStore
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime

interface RootTaskDependencyCoordinator {

    fun getDependencies(rootTaskRecord: RootTaskRecord): JsonTime.UserCustomTimeProvider

    class Impl(
        private val rootTaskKeyStore: RootTaskKeyStore,
        private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
    ) : RootTaskDependencyCoordinator {

        override fun getDependencies(rootTaskRecord: RootTaskRecord): JsonTime.UserCustomTimeProvider {
            rootTaskKeyStore.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getAllDependencyTaskKeys())

            return userCustomTimeProviderSource.getUserCustomTimeProvider(rootTaskRecord)
        }
    }
}