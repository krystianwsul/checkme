package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TimeLogger
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface ProjectToRootTaskCoordinator {

    fun getRootTasks(projectTracker: LoadDependencyTrackerManager.ProjectTracker): Completable

    class Impl(
        private val rootTaskKeySource: RootTaskKeySource,
        private val rootTasksFactory: RootTasksFactory,
        private val rootTaskDependencyStateContainer: RootTaskDependencyStateContainer,
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
        ): Boolean {
            val tracker = TimeLogger.start("ProjectToRootTaskCoordinator.hasAllDependentTasks")
            val isComplete = checkTaskKeys.all(rootTaskDependencyStateContainer::isComplete)
            tracker.stop("branch")

            return isComplete
        }
    }
}