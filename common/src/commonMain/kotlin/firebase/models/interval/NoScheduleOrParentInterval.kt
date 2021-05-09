package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.noscheduleorparent.NoScheduleOrParent
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset

class NoScheduleOrParentInterval(
    override val startExactTimeStampOffset: ExactTimeStamp.Offset,
    override val endExactTimeStampOffset: ExactTimeStamp.Offset?,
    val noScheduleOrParent: NoScheduleOrParent,
) : CurrentOffset