package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface ProjectToRootTaskCoordinator {

    fun getRootTasks(projectTracker: LoadDependencyTrackerManager.ProjectTracker): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            private val rootTasksFactory: RootTasksFactory,
    ) : ProjectToRootTaskCoordinator {

        override fun getRootTasks(projectTracker: LoadDependencyTrackerManager.ProjectTracker): Completable {
            rootTaskKeySource.onProjectAddedOrUpdated(projectTracker.projectKey, projectTracker.dependentTaskKeys)

            val loadedKeys = mutableSetOf<TaskKey.Root>()

            return Completable.merge(
                    projectTracker.dependentTaskKeys.map { getTaskDependenciesLoadedCompletable(it, loadedKeys) }
            )
        }

        private fun getTaskLoadedSingle(taskKey: TaskKey.Root): Single<RootTask> {
            return Observable.just(Unit)
                    .concatWith(rootTasksFactory.unfilteredChanges)
                    .mapNotNull { rootTasksFactory.rootTasks[taskKey] }
                    .firstOrError()
        }

        private fun getTaskDependenciesLoadedCompletable(
                taskKey: TaskKey.Root,
                loadedTaskKeys: MutableSet<TaskKey.Root>,
        ): Completable {
            return getTaskLoadedSingle(taskKey).flatMapCompletable {
                loadedTaskKeys += it.taskKey

                val newTaskKeys = it.taskRecord.getDependentTaskKeys() - loadedTaskKeys

                Completable.merge(newTaskKeys.map { getTaskDependenciesLoadedCompletable(it, loadedTaskKeys) })
            }
        }
    }
}