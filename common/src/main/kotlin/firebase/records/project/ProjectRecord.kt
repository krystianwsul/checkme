package com.krystianwsul.common.firebase.records.project

import com.krystianwsul.common.firebase.json.projects.ProjectJson
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.RootTaskParentDelegate
import com.krystianwsul.common.firebase.records.task.ProjectTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

@Suppress("LeakingThis")
abstract class ProjectRecord<T : ProjectType>(
    create: Boolean,
    private val projectJson: ProjectJson,
    private val _id: ProjectKey<T>,
    protected val committerKey: String,
) : RemoteRecord(create) {

    companion object {

        const val PROJECT_JSON = "projectJson"
    }

    abstract val projectKey: ProjectKey<T>

    final override val key get() = _id.key

    abstract val childKey: String

    val rootTaskParentDelegate = object : RootTaskParentDelegate(projectJson) {

        override fun addValue(subKey: String, value: Boolean?) {
            this@ProjectRecord.addValue("$committerKey/$subKey", value)
        }
    }

    abstract val taskRecords: Map<String, ProjectTaskRecord>
}
