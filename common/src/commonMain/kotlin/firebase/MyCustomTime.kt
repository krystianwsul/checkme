package com.krystianwsul.common.firebase

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Endable

interface MyCustomTime : Endable {

    override var endExactTimeStamp: ExactTimeStamp.Local?
}