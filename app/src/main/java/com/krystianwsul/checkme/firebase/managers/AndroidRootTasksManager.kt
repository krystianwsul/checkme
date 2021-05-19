package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.managers.RootTasksManager
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey

class AndroidRootTasksManager(databaseWrapper: DatabaseWrapper) : RootTasksManager(databaseWrapper) {

    companion object {

        private fun Snapshot<RootTaskJson>.toKey() = TaskKey.Root(key)
    }

    fun set(snapshot: Snapshot<RootTaskJson>) = set(
            snapshot.toKey(),
            { it.createObject != snapshot.value },
            {
                /**
                 * In principle, this if { exists } is fine.  Once loaded, a root task should disappear only once it's
                 * garbage collected by the backend, at which point it wasn't being used for anything, anyway - by
                 * definition.
                 *
                 * There is, however, an edge case where it does make a difference: if the backend mistakenly garbage
                 * collects the task, but doesn't remove its reference in Project.rootTaskIds, the app will keep hobbling
                 * along with the old data, without knowing that it's technically in an inconsistent state.
                 */
                snapshot.takeIf { it.exists }?.let {
                    RootTaskRecord(
                        it.key,
                        it.value!!,
                        databaseWrapper,
                        this,
                    )
                }
            },
    )?.data
}