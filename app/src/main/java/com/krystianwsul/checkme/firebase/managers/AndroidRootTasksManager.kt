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