package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute

interface InstanceRecord<out T> {

    val scheduleYear: Int
    val scheduleMonth: Int
    val scheduleDay: Int

    val scheduleCustomTimeId: T?
    val scheduleHour: Int?
    val scheduleMinute: Int?

    val instanceDate: Date?

    val instanceCustomTimeId: T?
    val instanceHourMinute: HourMinute?

    val done: Long?

    var ordinal: Double?
}