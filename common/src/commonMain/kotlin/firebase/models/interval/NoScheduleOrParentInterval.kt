package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParentInterval<T : ProjectType>(
        override val startExactTimeStamp: ExactTimeStamp,
        override val endExactTimeStamp: ExactTimeStamp?,
        val noScheduleOrParent: NoScheduleOrParent<T>
) : Current