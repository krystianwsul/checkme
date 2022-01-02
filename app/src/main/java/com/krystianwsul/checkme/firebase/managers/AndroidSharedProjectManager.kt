package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(databaseWrapper: DatabaseWrapper) :
    SharedProjectManager(databaseWrapper),
    ProjectProvider.ProjectManager<ProjectType.Shared, JsonWrapper> {

    private fun Snapshot<*>.toKey() = ProjectKey.Shared(key)

    private fun Snapshot<JsonWrapper>.toRecord() = SharedProjectRecord(
        this@AndroidSharedProjectManager,
        toKey(),
        value!!,
    )

    override fun set(snapshot: Snapshot<JsonWrapper>) = set(
            snapshot.toKey(),
            { it.createObject != snapshot.value },
            { snapshot.takeIf { it.exists }?.toRecord() },
    )
}