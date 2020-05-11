package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(override val databaseWrapper: DatabaseWrapper) :
        SharedProjectManager(),
        ProjectProvider.ProjectManager<ProjectType.Shared> {

    private fun Snapshot.toRecord() = SharedProjectRecord(
            databaseWrapper,
            this@AndroidSharedProjectManager,
            ProjectKey.Shared(key),
            getValue(JsonWrapper::class.java)!!
    )

    override fun setProjectRecord(snapshot: Snapshot) = set(ProjectKey.Shared(snapshot.key)) {
        snapshot.takeIf { it.exists() }?.toRecord()
    }
}