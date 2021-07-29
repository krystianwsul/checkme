package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.TaskKey
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
            rootTaskKeySource.onRootTaskAddedOrUpdated(rootTaskRecord.taskKey, rootTaskRecord.getAllDependencyTaskKeys())

            return listOf(
                Observable.just(Unit),
                rootTasksLoader.allEvents,
                userCustomTimeProviderSource.getTimeChangeObservable(),
            ).merge()
                .filter {
                    val (hasTasks, checkedTaskKeys) = hasTasks(rootTaskRecord)

                    hasTasks && hasTimes(rootTaskRecord, checkedTaskKeys)
                }
                .firstOrError()
                .flatMap { userCustomTimeProviderSource.getUserCustomTimeProvider(rootTaskRecord) } // this will be instance
        }

        private fun hasTasks(rootTaskRecord: RootTaskRecord) =
            rootTaskDependencyStateContainer.isComplete(rootTaskRecord.taskKey)

        private fun hasTimes(
            rootTaskRecord: RootTaskRecord,
            supposedlyPresentTaskKeys: Set<TaskKey.Root>,
            checkedTaskKeys: MutableSet<TaskKey.Root> = mutableSetOf(),
        ): Boolean {
            if (!userCustomTimeProviderSource.hasCustomTimes(rootTaskRecord)) return false

            val dependentTaskKeys = rootTaskRecord.getAllDependencyTaskKeys()
            val uncheckedTaskKeys = dependentTaskKeys - checkedTaskKeys

            val uncheckedTasks = uncheckedTaskKeys.associateWith {
                taskRecordLoader.tryGetTaskRecord(it) ?: throw MissingRecordException(
                    "current key: $it, " +
                            "dependentTaskKeys: $dependentTaskKeys, " +
                            "previously checked keys: $supposedlyPresentTaskKeys"
                )
            }

            checkedTaskKeys += uncheckedTaskKeys

            return uncheckedTasks.asSequence()
                .map { hasTimes(it.value, supposedlyPresentTaskKeys, checkedTaskKeys) }
                .all { it }
        }

        private class MissingRecordException(message: String) : Exception(message)
    }
}