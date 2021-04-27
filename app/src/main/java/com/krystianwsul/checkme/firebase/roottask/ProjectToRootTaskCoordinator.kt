package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface ProjectToRootTaskCoordinator {

    fun getRootTasks(
            projectTracker: LoadDependencyTrackerManager.ProjectTracker,
            projectRecord: ProjectRecord<*>,
    ): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            private val rootTasksFactory: RootTasksFactory,
    ) : ProjectToRootTaskCoordinator {

        override fun getRootTasks(
                projectTracker: LoadDependencyTrackerManager.ProjectTracker,
                projectRecord: ProjectRecord<*>,
        ): Completable {
            rootTaskKeySource.onProjectAddedOrUpdated(
                    projectRecord.projectKey,
                    projectRecord.rootTaskParentDelegate.rootTaskKeys,
            )

            val loadedKeys = mutableSetOf<TaskKey.Root>()

            return Completable.merge(
                    projectRecord.rootTaskParentDelegate
                            .rootTaskKeys
                            .map { getTaskDependenciesLoadedCompletable(projectTracker, it, loadedKeys) }
            )
        }

        private fun getTaskLoadedSingle(
                projectTracker: LoadDependencyTrackerManager.ProjectTracker,
                taskKey: TaskKey.Root,
        ): Single<RootTask> {
            projectTracker.addTaskKey(taskKey)

            return Observable.just(Unit)
                    .concatWith(rootTasksFactory.unfilteredChanges)
                    .mapNotNull { rootTasksFactory.rootTasks[taskKey] }
                    .firstOrError()
        }

        private fun getTaskDependenciesLoadedCompletable(
                projectTracker: LoadDependencyTrackerManager.ProjectTracker,
                taskKey: TaskKey.Root,
                loadedTaskKeys: MutableSet<TaskKey.Root>,
        ): Completable {
            return getTaskLoadedSingle(projectTracker, taskKey).flatMapCompletable {
                loadedTaskKeys += it.taskKey

                val newTaskKeys = it.taskRecord.getDependentTaskKeys() - loadedTaskKeys

                Completable.merge(newTaskKeys.map { getTaskDependenciesLoadedCompletable(projectTracker, it, loadedTaskKeys) })
            }
        }
    }
}