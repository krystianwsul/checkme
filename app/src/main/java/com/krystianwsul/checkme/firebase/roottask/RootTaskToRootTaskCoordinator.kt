package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface RootTaskToRootTaskCoordinator {

    fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            rootTasksLoader: RootTasksLoader,
            domainDisposable: CompositeDisposable,
            private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
    ) : RootTaskToRootTaskCoordinator {

        private val taskRecordLoader = RecursiveTaskRecordLoader.TaskRecordLoader.Impl(rootTasksLoader, domainDisposable)

        override fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable {
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getDependentTaskKeys())

            return RecursiveTaskRecordLoader(
                    rootTaskRecord,
                    taskRecordLoader,
                    rootTaskUserCustomTimeProviderSource,
            ).completable
        }
    }
}