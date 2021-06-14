package com.krystianwsul.checkme.firebase.roottask

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
                .filter { projectTracker.dependentTaskKeys.all(rootTaskDependencyStateContainer::isComplete) }
                .firstOrError()
                .ignoreElement()
        }
    }
}