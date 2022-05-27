package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.projects.ProjectsManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(databaseWrapper: DatabaseWrapper) :
    SharedProjectManager(databaseWrapper),
    ProjectsManager<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord> {

    override fun remove(projectKey: ProjectKey<out ProjectType.Shared>) {
        super.remove(projectKey as ProjectKey.Shared)
    }

    override fun set(
        projectKey: ProjectKey<out ProjectType.Shared>,
        snapshot: Snapshot<out JsonWrapper>
    ): SharedOwnedProjectRecord? {
        return set(
            projectKey as ProjectKey.Shared,
            { it.createObject != snapshot.value },
            {
                snapshot.value?.let { SharedOwnedProjectRecord(this, projectKey, it) }
            },
        )
    }
}