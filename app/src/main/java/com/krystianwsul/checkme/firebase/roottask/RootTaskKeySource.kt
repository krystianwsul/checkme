package com.krystianwsul.checkme.firebase.roottask

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

class RootTaskKeySource(private val domainDisposable: CompositeDisposable) {

    private val projectEvents = PublishRelay.create<ProjectEvent>()

    val rootTaskKeysObservable: Observable<Set<TaskKey.Root>> =
            projectEvents.scan(ProjectAggregate()) { aggregate, projectEvent ->
                when (projectEvent) {
                    is ProjectEvent.ProjectAddedOrUpdated -> aggregate.copy(
                            aggregate.projectMap
                                    .toMutableMap()
                                    .apply { put(projectEvent.projectKey, projectEvent.rootTaskKeys) }
                    )
                    is ProjectEvent.ProjectsRemoved -> {
                        val newMap = aggregate.projectMap.toMutableMap()

                        projectEvent.projectKeys.forEach {
                            check(newMap.containsKey(it))

                            newMap.remove(it)
                        }

                        aggregate.copy(newMap)
                    }
                }
            }
                    .skip(1)
                    .map { it.output }
                    .distinctUntilChanged()

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) {
        // this covers:
        // private project: remote initial load, remote changes to tasks
        // shared project: both remote and local initial load, remote changes to tasks

        // this contributes to final observable by adding keys, or updating keys for given project

        projectEvents.accept(ProjectEvent.ProjectAddedOrUpdated(projectKey, rootTaskKeys))
    }

    fun onTaskAddedLocally(parentKey: ParentKey, taskKey: TaskKey, taskRecord: RootTaskRecord) {
        // todo task fetch not sure if this will require rootTaskRecord; depends on RootTaskManager impl

        // this covers task being added locally.  Not sure if it will need to return anything
    }

    fun onTaskParentChanged(oldParentKey: ParentKey, newParentKey: ParentKey, taskKey: TaskKey.Root) {
        // todo task fetch
    }

    fun onProjectsRemoved(projectKeys: Set<ProjectKey<*>>) {
        projectEvents.accept(ProjectEvent.ProjectsRemoved(projectKeys))
    }

    /**
     * todo task fetch add callbacks for recursive tasks, similar to projects.
     */

    /**
     * todo task fetch custom times, similar to projects. We will need to load custom times in response to remote
     * changes, including initial remote loads.  But not local edits, since those can only assign our own custom times.
     * But local edits may affect bookkeeping, in that changing a time on a task may make a certain custom time no
     * longer needed.
     */

    sealed class ParentKey {

        data class Project(val projectKey: ProjectKey<*>) : ParentKey()
        data class Task(val taskKey: TaskKey.Root) : ParentKey()
    }

    private sealed class ProjectEvent {

        data class ProjectAddedOrUpdated(
                val projectKey: ProjectKey<*>,
                val rootTaskKeys: Set<TaskKey.Root>,
        ) : ProjectEvent()

        data class ProjectsRemoved(val projectKeys: Set<ProjectKey<*>>) : ProjectEvent()
    }

    private data class ProjectAggregate(val projectMap: Map<ProjectKey<*>, Set<TaskKey.Root>> = mapOf()) {

        val output = projectMap.values
                .flatten()
                .toSet()
    }
}