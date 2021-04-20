package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParentInterval<T : ProjectType>(
        override val startExactTimeStampOffset: ExactTimeStamp.Offset,
        override val endExactTimeStampOffset: ExactTimeStamp.Offset?,
        val noScheduleOrParent: NoScheduleOrParent,
) : CurrentOffset