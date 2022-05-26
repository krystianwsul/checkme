package com.krystianwsul.common.firebase.models.customtime

import com.krystianwsul.common.firebase.MyCustomTime
import com.krystianwsul.common.firebase.models.project.PrivateOwnedProject
import com.krystianwsul.common.firebase.records.customtime.PrivateCustomTimeRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.ProjectType

class PrivateCustomTime(
    override val project: PrivateOwnedProject,
    override val customTimeRecord: PrivateCustomTimeRecord,
) : Time.Custom.Project<ProjectType.Private>(), MyCustomTime {

    override val key = customTimeRecord.customTimeKey
    override val id = key.customTimeId

    override val notDeleted: Boolean
        get() {
            val current = customTimeRecord.current
            check(endExactTimeStamp == null || !current)

            return endExactTimeStamp == null || current
        }

    override var endExactTimeStamp
        get() = customTimeRecord.endTime?.let { ExactTimeStamp.Local(it) }
        set(value) {
            check((value == null) != (customTimeRecord.endTime == null))

            customTimeRecord.current = value == null
            customTimeRecord.endTime = value?.long
        }
}
