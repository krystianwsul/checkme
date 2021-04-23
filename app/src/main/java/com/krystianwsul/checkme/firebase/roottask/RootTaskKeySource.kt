package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.RequestKeyStore
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

class RootTaskKeySource(private val domainDisposable: CompositeDisposable) {

    private val projectStore = RequestKeyStore<ProjectKey<*>, TaskKey.Root>()
    private val taskStore = RequestKeyStore<TaskKey.Root, TaskKey.Root>()

    val rootTaskKeysObservable: Observable<Set<TaskKey.Root>> = RequestKeyStore.merge(projectStore, taskStore)

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) {
        // this covers:
        // private project: remote initial load, remote changes to tasks
        // shared project: both remote and local initial load, remote changes to tasks

        // this contributes to final observable by adding keys, or updating keys for given project

        projectStore.requestCustomTimeUsers(projectKey, rootTaskKeys)
    }

    fun onTaskAddedLocally(parentKey: ParentKey, taskKey: TaskKey, taskRecord: RootTaskRecord) {
        // todo double-check that local changes to the keys that a project task aren't propagated through that other place

        // todo task fetch not sure if this will require rootTaskRecord; depends on RootTaskManager impl

        // this covers task being added locally.  Not sure if it will need to return anything
    }

    fun onTaskParentChanged(oldParentKey: ParentKey, newParentKey: ParentKey, taskKey: TaskKey.Root) {
        // todo task fetch
    }

    fun onProjectsRemoved(projectKeys: Set<ProjectKey<*>>) = projectStore.onRequestsRemoved(projectKeys)

    fun onRootTaskAddedOrUpdated(parentRootTaskKey: TaskKey.Root, childRootTaskKeys: Set<TaskKey.Root>) {
        // this covers:
        // private project: remote initial load, remote changes to tasks
        // shared project: both remote and local initial load, remote changes to tasks

        // this contributes to final observable by adding keys, or updating keys for given project

        // todo task fetch projectEvents.accept(ProjectEvent.ProjectAddedOrUpdated(projectKey, rootTaskKeys))
    }

    fun onRootTasksRemoved(rootTaskKeys: Set<TaskKey.Root>) {
        // todo task fetch projectEvents.accept(ProjectEvent.ProjectsRemoved(rootTaskKeys))
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
}