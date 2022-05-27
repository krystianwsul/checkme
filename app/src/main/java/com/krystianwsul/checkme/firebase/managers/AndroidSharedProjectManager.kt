package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(databaseWrapper: DatabaseWrapper) :
    SharedProjectManager(databaseWrapper),
    ProjectProvider.ProjectManager<ProjectType.Shared, JsonWrapper> {

    override fun set(snapshot: Snapshot<JsonWrapper>): SharedOwnedProjectRecord? {
        val projectKey = ProjectKey.Shared(snapshot.key)

        return set(
            projectKey,
            { it.createObject != snapshot.value },
            {
                snapshot.value?.let { SharedOwnedProjectRecord(this, projectKey, it) }
            },
        )
    }
}