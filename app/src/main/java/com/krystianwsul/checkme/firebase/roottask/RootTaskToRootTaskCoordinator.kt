package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import io.reactivex.rxjava3.core.Completable

interface RootTaskToRootTaskCoordinator {

    fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable

    class Impl(private val rootTaskKeySource: RootTaskKeySource) : RootTaskToRootTaskCoordinator {

        override fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable {
            rootTaskKeySource.onRootTaskAddedOrUpdated(
                    rootTaskRecord.taskKey,
                    rootTaskRecord.rootTaskParentDelegate.rootTaskKeys,
            )

            return Completable.complete() // todo task fetch return after tasks are loaded
        }
    }
}