package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.ForeignProjectJson
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

@Suppress("LeakingThis")
abstract class ForeignProjectRecord<T : ProjectType>(
    projectJson: ForeignProjectJson,
    _id: ProjectKey<T>,
    committerKey: String,
) : ProjectRecord<T>(false, projectJson, _id, committerKey) {

    final override val createObject: Any get() = throw UnsupportedOperationException()

    override val taskRecords = mapOf<String, ProjectTaskRecord>()

    final override fun deleteFromParent() = throw UnsupportedOperationException()
}
