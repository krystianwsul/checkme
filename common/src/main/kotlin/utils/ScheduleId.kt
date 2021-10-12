package com.krystianwsul.common.utils

import kotlin.jvm.JvmInline

@JvmInline
value class ScheduleId(val value: String) : Comparable<ScheduleId> {

    override fun compareTo(other: ScheduleId) = value.compareTo(other.value)

    override fun toString() = value
}