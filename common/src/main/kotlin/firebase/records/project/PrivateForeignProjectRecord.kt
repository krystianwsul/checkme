package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.PrivateForeignProjectJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class PrivateForeignProjectRecord(
    override val projectKey: ProjectKey.Private,
    projectJson: PrivateForeignProjectJson,
) : ForeignProjectRecord<ProjectType.Private>(projectJson, projectKey, projectKey.key) {

    override val childKey get() = key
}