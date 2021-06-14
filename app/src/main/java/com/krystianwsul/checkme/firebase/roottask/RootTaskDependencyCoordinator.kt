package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TimeLogger
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
        private val rootTaskDependencyStateContainer: RootTaskDependencyStateContainer,
    ) : RootTaskDependencyCoordinator {

        override fun getDependencies(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider> {
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getDependentTaskKeys())

            return listOf(
                Observable.just(Unit),
                rootTasksLoader.allEvents,
                userCustomTimeProviderSource.getTimeChangeObservable(),
            ).merge()
                .filter { hasTasks(rootTaskRecord) && hasTimes(rootTaskRecord) }
                .firstOrError()
                .flatMap { userCustomTimeProviderSource.getUserCustomTimeProvider(rootTaskRecord) } // this will be instance
        }

        private fun hasTasks(rootTaskRecord: RootTaskRecord): Boolean {
            val tracker = TimeLogger.start("RootTaskDependencyCoordinator.hasTasks")
            val isComplete = rootTaskDependencyStateContainer.isComplete(rootTaskRecord.taskKey)
            tracker.stop()

            return isComplete
        }

        private fun hasTimes(
            rootTaskRecord: RootTaskRecord,
            checkedTaskKeys: MutableSet<TaskKey.Root> = mutableSetOf(),
        ): Boolean {
            val tracker = TimeLogger.start("RootTaskDependencyCoordinator.hasTimes")
            if (!userCustomTimeProviderSource.hasCustomTimes(rootTaskRecord)) {
                tracker.stop("branch 1")
                return false
            }

            val dependentTaskKeys = rootTaskRecord.getDependentTaskKeys()
            val uncheckedTaskKeys = dependentTaskKeys - checkedTaskKeys

            val uncheckedTasks = uncheckedTaskKeys.associateWith { taskRecordLoader.tryGetTaskRecord(it)!! }

            checkedTaskKeys += uncheckedTaskKeys

            return uncheckedTasks.asSequence()
                .also {
                    tracker.stop("branch 3")
                }
                .map { hasTimes(it.value, checkedTaskKeys) }
                .all { it }
        }
    }
}