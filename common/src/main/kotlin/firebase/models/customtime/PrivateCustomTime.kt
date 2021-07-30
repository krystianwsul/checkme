package com.krystianwsul.common.firebase.models.customtime

import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.records.customtime.PrivateCustomTimeRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectType

class PrivateCustomTime(
        override val project: PrivateProject,
        override val customTimeRecord: PrivateCustomTimeRecord,
) : Time.Custom.Project<ProjectType.Private>(), MyCustomTime {

    override val key = customTimeRecord.customTimeKey
    override val id = key.customTimeId

    override fun notDeleted(exactTimeStamp: ExactTimeStamp.Local?): Boolean {
        return if (exactTimeStamp != null) {
            val current = customTimeRecord.current
            val endExactTimeStamp = endExactTimeStamp

            check(endExactTimeStamp == null || !current)

            endExactTimeStamp?.let { it > exactTimeStamp } ?: current
        } else {
            val current = customTimeRecord.current
            check(endExactTimeStamp == null || !current)

            endExactTimeStamp == null || current
        }
    }

    override var endExactTimeStamp
        get() = customTimeRecord.endTime?.let { ExactTimeStamp.Local(it) }
        set(value) {
            check((value == null) != (customTimeRecord.endTime == null))

            customTimeRecord.current = value == null
            customTimeRecord.endTime = value?.long
        }
}
