package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TimeLogger
import com.krystianwsul.common.utils.filterValuesNotNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.merge

interface RootTaskDependencyCoordinator {

    fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider>

    class Impl(
        private val rootTaskKeySource: RootTaskKeySource,
        private val rootTasksLoader: RootTasksLoader,
        private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
        private val taskRecordLoader: TaskRecordsLoadedTracker,
    ) : RootTaskDependencyCoordinator {

        override fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider> {
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getDependentTaskKeys())

            return listOf(
                Observable.just(Unit),
                rootTasksLoader.allEvents,
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
            val tracker = TimeLogger.start("RootTaskDependencyCoordinator.hasDependencies")
            if (!userCustomTimeProviderSource.hasCustomTimes(rootTaskRecord)) {
                tracker.stop("branch 1")
                return false
            }

            val dependentTaskKeys = rootTaskRecord.getDependentTaskKeys()
            val uncheckedTaskKeys = dependentTaskKeys - checkedTaskKeys

            val uncheckedTasks =
                uncheckedTaskKeys.associateWith { taskRecordLoader.tryGetTaskRecord(it) }.filterValuesNotNull()

            if (!uncheckedTasks.keys.containsAll(uncheckedTaskKeys)) {
                tracker.stop("branch 2")
                return false
            }

            checkedTaskKeys += uncheckedTaskKeys

            return uncheckedTasks.asSequence()
                .also {
                    tracker.stop("branch 3")
                }
                .map { hasDependencies(it.value, checkedTaskKeys) }
                .all { it }
        }
    }
}