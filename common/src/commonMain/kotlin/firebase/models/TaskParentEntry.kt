package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current

interface TaskParentEntry : Current {

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp)
}