package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface RootTaskDependencyCoordinator {

    fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider>

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            rootTasksLoader: RootTasksLoader,
            private val domainDisposable: CompositeDisposable,
            private val rootTaskUserCustomTimeProviderSource: RootTaskUserCustomTimeProviderSource,
    ) : RootTaskDependencyCoordinator {

        private val taskRecordLoader =
                RecursiveTaskRecordLoader.TaskRecordLoader.Impl(rootTasksLoader, domainDisposable)

        override fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider> {
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getDependentTaskKeys())

            return Single.zip(
                    RecursiveTaskRecordLoader(
                            rootTaskRecord,
                            taskRecordLoader,
                            rootTaskUserCustomTimeProviderSource,
                            domainDisposable,
                    ).completable.toSingleDefault(Unit),
                    rootTaskUserCustomTimeProviderSource.getUserCustomTimeProvider(rootTaskRecord),
            ) { _, userCustomTimeProvider -> userCustomTimeProvider }
        }
    }
}