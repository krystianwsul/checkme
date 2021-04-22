package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.disposables.CompositeDisposable

class RootTaskKeySource(private val domainDisposable: CompositeDisposable) {

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) {
        // this covers:
        // private project: remote initial load, remote changes to tasks
        // shared project: both remote and local initial load, remote changes to tasks

        // this contributes to final observable by adding keys, or updating keys for given project
    }

    fun onTaskAddedLocally(parentKey: ParentKey, taskKey: TaskKey, taskRecord: RootTaskRecord) {
        // todo task fetch not sure if this will require rootTaskRecord; depends on RootTaskManager impl

        // this covers task being added locally.  Not sure if it will need to return anything
    }

    fun onTaskParentChanged(oldParentKey: ParentKey, newParentKey: ParentKey, taskKey: TaskKey.Root) {
        // todo task fetch
    }

    fun onProjectsRemoved(projectKeys: Set<ProjectKey<*>>) {
        // todo task fetch
    }

    /**
     * todo task fetch add callbacks for recursive tasks, similar to projects.
     */

    // todo task fetch custom times, similar to projects

    sealed class ParentKey {

        data class Project(val projectKey: ProjectKey<*>) : ParentKey()
        data class Task(val taskKey: TaskKey.Root) : ParentKey()
    }
}