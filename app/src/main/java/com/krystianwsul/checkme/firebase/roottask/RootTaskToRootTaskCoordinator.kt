package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface RootTaskToRootTaskCoordinator {

    fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            rootTaskLoader: RootTaskLoader,
            domainDisposable: CompositeDisposable,
            private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
    ) : RootTaskToRootTaskCoordinator {

        private val taskRecordLoader = RecursiveTaskRecordLoader.TaskRecordLoader.Impl(rootTaskLoader, domainDisposable)

        override fun getRootTasks(rootTaskRecord: RootTaskRecord): Completable {
            val allRelatedTasks: Set<TaskKey.Root> = rootTaskRecord.rootTaskParentDelegate.rootTaskKeys +
                    rootTaskRecord.taskHierarchyRecords.map { TaskKey.Root(it.value.parentTaskId) }

            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, allRelatedTasks)

            return RecursiveTaskRecordLoader(
                    rootTaskRecord,
                    taskRecordLoader,
                    rootTaskUserCustomTimeProviderSource,
            ).completable
        }
    }
}