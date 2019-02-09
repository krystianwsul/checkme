package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.time.Date

interface InstanceRecord<out T> {

    val scheduleYear: Int
    val scheduleMonth: Int
    val scheduleDay: Int

    val scheduleCustomTimeId: T?
    val scheduleHour: Int?
    val scheduleMinute: Int?

    val instanceDate: Date?

    val instanceCustomTimeId: T?
    val instanceHour: Int?
    val instanceMinute: Int?

    val done: Long?

    var ordinal: Double?
}