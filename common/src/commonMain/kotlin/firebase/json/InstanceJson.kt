package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class InstanceJson @JvmOverloads constructor(
        var done: Long? = null,
        var instanceDate: String? = null,
        var instanceYear: Int? = null,
        var instanceMonth: Int? = null,
        var instanceDay: Int? = null,
        var instanceTime: String? = null,
        var instanceCustomTimeId: String? = null,
        var instanceHour: Int? = null,
        var instanceMinute: Int? = null,
        var ordinal: Double? = null,
        var hidden: Boolean = false)