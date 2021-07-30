package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.CurrentOffset

interface TaskParentEntry : Current, CurrentOffset {

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp)

    fun clearEndExactTimeStamp(now: ExactTimeStamp.Local)
}