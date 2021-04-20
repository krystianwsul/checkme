package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.models.PrivateCustomTime
import com.krystianwsul.common.time.CustomTimeProperties
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.Endable

interface MyCustomTime : CustomTimeProperties, Endable {

    override var endExactTimeStamp: ExactTimeStamp.Local?

    fun setHourMinute(
            allRecordsSource: PrivateCustomTime.AllRecordsSource,
            dayOfWeek: DayOfWeek,
            hourMinute: HourMinute,
    )

    fun setName(allRecordsSource: PrivateCustomTime.AllRecordsSource, name: String)
}