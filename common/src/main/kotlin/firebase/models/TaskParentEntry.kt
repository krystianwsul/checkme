package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.Endable

interface TaskParentEntry : Endable, CurrentOffset {

    fun setEndExactTimeStamp(endExactTimeStamp: ExactTimeStamp)

    fun clearEndExactTimeStamp()
}