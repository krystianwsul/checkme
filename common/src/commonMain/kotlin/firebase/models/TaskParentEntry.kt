package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.CurrentDateTime

interface TaskParentEntry : Current, CurrentDateTime {

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp)
}