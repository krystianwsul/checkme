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

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) =
            projectStore.addRequest(projectKey, rootTaskKeys)

    fun onProjectsRemoved(projectKeys: Set<ProjectKey<*>>) = projectStore.onRequestsRemoved(projectKeys)

    fun onTaskAddedLocally(parentKey: ParentKey, taskKey: TaskKey, taskRecord: RootTaskRecord) {
        // todo task create not sure if this will require rootTaskRecord; depends on RootTaskManager impl

        // see notes for onRootTaskAddedOrUpdated
    }

    fun onTaskParentChanged(oldParentKey: ParentKey, newParentKey: ParentKey, taskKey: TaskKey.Root) {
        // todo task edit
    }

    fun onRootTaskAddedOrUpdated(parentRootTaskKey: TaskKey.Root, childRootTaskKeys: Set<TaskKey.Root>) {
        // this happens for initial loads, and when a task is changed remotely.

        /**
         * todo task create this is going to be tricky.  When we add a task locally, we want to set up the listeners
         * for it.  But I haven't decided if we want the change to propagate through the RX chain down to the factory,
         * or if we'll do manual book-keeping there.  I think that decision should come after we figure out what's
         * easiest for edits.
         */

        taskStore.addRequest(parentRootTaskKey, childRootTaskKeys)
    }

    fun onRootTasksRemoved(rootTaskKeys: Set<TaskKey.Root>) = taskStore.onRequestsRemoved(rootTaskKeys)

    /**
     * OLD: custom times, similar to projects. We will need to load custom times in response to remote
     * changes, including initial remote loads.  But not local edits, since those can only assign our own custom times.
     * But local edits may affect bookkeeping, in that changing a time on a task may make a certain custom time no
     * longer needed.
     *
     * todo task edit If we end up do fine-grained updates for local edits to tasks (for child task keys), consider
     * doing that as well for custom times.  The two cases are very similar, so let's be thorough.
     */

    sealed class ParentKey {

        data class Project(val projectKey: ProjectKey<*>) : ParentKey()
        data class Task(val taskKey: TaskKey.Root) : ParentKey()
    }
}