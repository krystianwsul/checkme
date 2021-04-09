package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType


abstract class ProjectCustomTimeRecord<T : ProjectType>(create: Boolean) : CustomTimeRecord<T>(create) {

    abstract override val id: CustomTimeId.Project<T>
    abstract override val customTimeKey: CustomTimeKey.Project<T>
    abstract override val projectRecord: ProjectRecord<T>
}
