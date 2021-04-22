package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable

class RootTaskKeySource(private val domainDisposable: CompositeDisposable) {

    fun onProjectAddedOrUpdated(projectKey: ProjectKey<*>, rootTaskKeys: Set<TaskKey.Root>) {
        // this covers:
        // private project: remote initial load, remote changes to tasks
        // shared project: both remote and local initial load, remote changes to tasks

        // this contributes to final observable by adding keys, or updating keys for given project
    }

    fun onAddTask(parentKey: ParentKey, taskKey: TaskKey, taskRecord: RootTaskRecord) {
        // todo task fetch not sure if this will require rootTaskRecord; depends on RootTaskManager impl

        // this covers task being added locally.  Not sure if it will need to return anything
    }

    fun onChangeTaskParent(oldParentKey: ParentKey, newParentKey: ParentKey, taskKey: TaskKey.Root) {
        // todo task fetch
    }

    /** todo task fetch
     * List of events that affect which root tasks need to be wired up:
     *
     * (done onProjectAddedOrUpdated) Initial private project load: completable, watch firebase
     * (done onProjectAddedOrUpdated) Initial shared project load (each): completable, watch firebase
     *
     * Project edits:
     * - (done onAddTask) Task added locally: immediate, watch firebase, feed initial task record through RX
     * - (done onProjectAddedOrUpdated) Task added remotely: completable: onProjectAddedOrUpdated
     * - Tasks removed remotely: stop watching firebase, remove from model
     * - (done onChangeTaskParent) Task project edited: atomically change bookkeeping for which project it belongs to
     *
     * Projects removed: immediate, stop watching firebase
     *
     * OH FUCK, what about recursive task loads?
     *
     * P.S.: this will need to factor into custom times, too!
     */

    fun onProjectAddedRemotely(projectKey: ProjectKey<*>, remoteTaskKeys: Set<TaskKey.Root>) {
        TODO("todo task fetch is this reasonable?")
    }

    fun onProjectRecordUpdatedRemotely(projectKey: ProjectKey<*>, remoteTaskKeys: Set<TaskKey.Root>): Completable {
        TODO("todo task fetch")
    }

    sealed class ParentKey {

        data class Project(val projectKey: ProjectKey<*>) : ParentKey()
        data class Task(val taskKey: TaskKey.Root) : ParentKey()
    }
}