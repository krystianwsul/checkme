package com.krystianwsul.common.domain

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.RemoteCustomTimeId
import com.krystianwsul.common.utils.ScheduleKey

interface InstanceRecord<T : RemoteCustomTimeId> { // todo instance merge

    val scheduleYear: Int
    val scheduleMonth: Int
    val scheduleDay: Int

    val scheduleCustomTimeId: T?
    val scheduleHour: Int?
    val scheduleMinute: Int?

    val scheduleKey: ScheduleKey

    var instanceDate: Date?

    var instanceJsonTime: JsonTime<T>?

    var done: Long?

    var ordinal: Double?

    var hidden: Boolean

    fun delete()
}