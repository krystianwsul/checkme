package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.checkme.firebase.snapshot.ValueSnapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(override val databaseWrapper: DatabaseWrapper) :
        SharedProjectManager(),
        ProjectProvider.ProjectManager<ProjectType.Shared, JsonWrapper> {

    private fun Snapshot.toKey() = ProjectKey.Shared(key)

    private fun ValueSnapshot.toRecord() = SharedProjectRecord(
            databaseWrapper,
            this@AndroidSharedProjectManager,
            toKey(),
            getValue(JsonWrapper::class.java)!!,
    )

    override fun set(snapshot: TypedSnapshot<JsonWrapper>) = setNullable(snapshot.toKey()) {
        snapshot.takeIf { it.exists() }?.toRecord()
    }
}