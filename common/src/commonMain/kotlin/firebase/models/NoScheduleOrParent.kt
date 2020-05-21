package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.NoScheduleOrParentRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParent<T : ProjectType>(private val noScheduleOrParentRecord: NoScheduleOrParentRecord<T>) : Current {

    override val startExactTimeStamp = ExactTimeStamp(noScheduleOrParentRecord.startTime)
    override val endExactTimeStamp get() = noScheduleOrParentRecord.endTime?.let(::ExactTimeStamp)

    // todo no schedule record add to invervalbuilder, invalidate task stuff, add in DomainFactory create calls
}