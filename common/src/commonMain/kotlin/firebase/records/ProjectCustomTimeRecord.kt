package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType


abstract class ProjectCustomTimeRecord<T : ProjectType>(create: Boolean) : CustomTimeRecord(create) {

    abstract override val id: CustomTimeId.Project<T>
    abstract override val customTimeKey: CustomTimeKey.Project<T> // todo customtime project
    protected abstract val projectRecord: ProjectRecord<T>

    val projectId get() = projectRecord.projectKey

    override val key get() = projectRecord.childKey + "/" + CUSTOM_TIMES + "/" + id
}
