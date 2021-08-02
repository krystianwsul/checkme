package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import io.reactivex.rxjava3.core.Single

interface RootTaskDependencyCoordinator { // todo dependencies final cleanup

    fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider>

    class Impl(
        private val rootTaskKeySource: RootTaskKeySource,
        private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
    ) : RootTaskDependencyCoordinator {

        override fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider> {
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getAllDependencyTaskKeys())

            return userCustomTimeProviderSource.getUserCustomTimeProvider(rootTaskRecord) // todo dependencies middle cleanup
        }
    }
}