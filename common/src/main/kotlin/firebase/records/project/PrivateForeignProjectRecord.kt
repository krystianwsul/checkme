package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class PrivateForeignProjectRecord(
    override val projectKey: ProjectKey.Private,
    private val projectJson: PrivateForeignProjectJson,
) : ForeignProjectRecord<ProjectType.Private>(projectJson, projectKey, projectKey.key), PrivateProjectRecord {

    override val childKey get() = key

    override val ownerName get() = projectJson.ownerName
}