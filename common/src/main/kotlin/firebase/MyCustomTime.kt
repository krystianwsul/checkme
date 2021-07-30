package com.krystianwsul.common.firebase

import com.krystianwsul.common.time.CustomTimeProperties
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Endable

interface MyCustomTime : CustomTimeProperties, Endable {

    override var endExactTimeStamp: ExactTimeStamp.Local?
}