package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface ProjectToRootTaskCoordinator {

    fun getRootTasks(projectRecord: ProjectRecord<*>): Completable

    class Impl(
            private val rootTaskKeySource: RootTaskKeySource,
            private val rootTasksFactory: RootTasksFactory,
            private val projectDependencyLoadTrackerManager: ProjectDependencyLoadTrackerManager,
    ) : ProjectToRootTaskCoordinator {

        override fun getRootTasks(projectRecord: ProjectRecord<*>): Completable {
            rootTaskKeySource.onProjectAddedOrUpdated(
                    projectRecord.projectKey,
                    projectRecord.rootTaskParentDelegate.rootTaskKeys,
            )

            val loadedKeys = mutableSetOf<TaskKey.Root>()

            val tracker = projectDependencyLoadTrackerManager.startTrackingProjectLoad(projectRecord.projectKey)

            return Completable.merge(
                    projectRecord.rootTaskParentDelegate
                            .rootTaskKeys
                            .map { getTaskDependenciesLoadedCompletable(tracker, it, loadedKeys) }
            ).doOnComplete { projectDependencyLoadTrackerManager.stopTrackingProjectLoad(tracker) }
        }

        private fun getTaskLoadedSingle(
                tracker: ProjectDependencyLoadTrackerManager.Tracker,
                taskKey: TaskKey.Root,
        ): Single<RootTask> {
            tracker.addTaskKey(taskKey)

            return Observable.just(Unit)
                    .concatWith(rootTasksFactory.unfilteredChanges)
                    .mapNotNull { rootTasksFactory.rootTasks[taskKey] }
                    .firstOrError()
        }

        private fun getTaskDependenciesLoadedCompletable(
                tracker: ProjectDependencyLoadTrackerManager.Tracker,
                taskKey: TaskKey.Root,
                loadedTaskKeys: MutableSet<TaskKey.Root>,
        ): Completable {
            return getTaskLoadedSingle(tracker, taskKey).flatMapCompletable {
                loadedTaskKeys += it.taskKey

                val newTaskKeys = it.taskRecord.getDependentTaskKeys() - loadedTaskKeys

                Completable.merge(newTaskKeys.map { getTaskDependenciesLoadedCompletable(tracker, it, loadedTaskKeys) })
            }
        }
    }
}