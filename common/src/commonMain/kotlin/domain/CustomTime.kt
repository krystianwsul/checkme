package com.krystianwsul.common.domain

import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.RemoteCustomTimeId

interface CustomTime : Time {

    val name: String

    val hourMinutes: Map<DayOfWeek, HourMinute>

    val customTimeKey: CustomTimeKey<*>

    fun setHourMinute(allRecordsSource: AllRecordsSource, dayOfWeek: DayOfWeek, hourMinute: HourMinute)

    fun setName(allRecordsSource: AllRecordsSource, name: String)

    interface AllRecordsSource {

        fun getSharedCustomTimes(privateCustomTimeId: RemoteCustomTimeId.Private): List<CustomTime>
    }
}
