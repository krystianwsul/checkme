package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.filterValuesNotNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

interface RootTaskDependencyCoordinator {

    fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider>

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            private val rootTasksLoader: RootTasksLoader,
            domainDisposable: CompositeDisposable,
            private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
    ) : RootTaskDependencyCoordinator {

        private val taskRecordLoader =
                RecursiveTaskRecordLoader.TaskRecordLoader.Impl(rootTasksLoader, domainDisposable) // todo task track consider injecting

        override fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider> {
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getDependentTaskKeys())

            return listOf(
                    Observable.just(Unit),
                    rootTasksLoader.addChangeEvents,
                    rootTasksLoader.removeEvents,
                    userCustomTimeProviderSource.getTimeChangeObservable(),
            ).merge()
                    .filter { hasDependencies(rootTaskRecord) }
                    .firstOrError()
                    .flatMap { userCustomTimeProviderSource.getUserCustomTimeProvider(rootTaskRecord) } // this will be instance
        }

        private fun hasDependencies(
                rootTaskRecord: RootTaskRecord,
                checkedTaskKeys: MutableSet<TaskKey.Root> = mutableSetOf(),
        ): Boolean {
            if (!userCustomTimeProviderSource.hasCustomTimes(rootTaskRecord)) return false

            val dependentTaskKeys = rootTaskRecord.getDependentTaskKeys()
            val uncheckedTaskKeys = dependentTaskKeys - checkedTaskKeys

            val uncheckedTasks =
                    uncheckedTaskKeys.associateWith { taskRecordLoader.tryGetTaskRecord(it) }.filterValuesNotNull()

            if (!uncheckedTasks.keys.containsAll(uncheckedTaskKeys)) return false

            checkedTaskKeys += uncheckedTaskKeys

            return uncheckedTasks.asSequence()
                    .map { hasDependencies(it.value, checkedTaskKeys) }
                    .all { it }
        }
    }
}