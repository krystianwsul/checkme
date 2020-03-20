package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class InstanceJson @JvmOverloads constructor(
        var done: Long? = null,
        var instanceDate: String? = null,
        var instanceTime: String? = null,
        var ordinal: Double? = null,
        var hidden: Boolean = false
)