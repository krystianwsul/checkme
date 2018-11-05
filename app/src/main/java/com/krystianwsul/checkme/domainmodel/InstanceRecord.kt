package com.krystianwsul.checkme.domainmodel

interface InstanceRecord<T> {

    val scheduleYear: Int
    val scheduleMonth: Int
    val scheduleDay: Int

    val scheduleCustomTimeId: T?
    val scheduleHour: Int?
    val scheduleMinute: Int?

    val instanceYear: Int?
    val instanceMonth: Int?
    val instanceDay: Int?

    val instanceCustomTimeId: T?
    val instanceHour: Int?
    val instanceMinute: Int?

    val done: Long?

    var ordinal: Double?
}