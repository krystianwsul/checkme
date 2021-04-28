package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.filterValuesNotNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface ProjectToRootTaskCoordinator {

    fun getRootTasks(projectTracker: LoadDependencyTrackerManager.ProjectTracker): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            private val rootTasksFactory: RootTasksFactory,
    ) : ProjectToRootTaskCoordinator {

        override fun getRootTasks(projectTracker: LoadDependencyTrackerManager.ProjectTracker): Completable {
            rootTaskKeySource.onProjectAddedOrUpdated(projectTracker.projectKey, projectTracker.dependentTaskKeys)

            return Observable.just(Unit)
                    .concatWith(rootTasksFactory.unfilteredChanges)
                    .filter { hasAllDependentTasks(projectTracker.dependentTaskKeys) }
                    .firstOrError()
                    .ignoreElement()
        }

        private fun hasAllDependentTasks(
                checkTaskKeys: Set<TaskKey.Root>,
                checkedTaskKeys: MutableSet<TaskKey.Root> = mutableSetOf(),
        ): Boolean {
            val uncheckedTaskKeys = checkTaskKeys - checkedTaskKeys

            val uncheckedTasks = uncheckedTaskKeys.associateWith {
                rootTasksFactory.rootTasks[it]
            }.filterValuesNotNull()

            if (!uncheckedTasks.keys.containsAll(uncheckedTaskKeys)) return false

            checkedTaskKeys += uncheckedTaskKeys

            return uncheckedTasks.asSequence()
                    .map { hasAllDependentTasks(it.value.taskRecord.getDependentTaskKeys(), checkedTaskKeys) }
                    .all { it }
        }
    }
}