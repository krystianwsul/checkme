package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Completable

class RootTaskKeySource {

    /** todo task fetch
     * List of events that affect which root tasks need to be wired up:
     *
     * Initial private project load: completable, watch firebase
     *
     * Initial shared project load (each): completable, watch firebase
     *
     * Project edits:
     * - Task added locally: immediate, watch firebase, feed initial task record through RX
     * - Task added remotely: completable
     * - Tasks removed remotely: stop watching firebase, remove from model
     * - Task project edited: atomically change bookkeeping for which project it belongs to
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
}