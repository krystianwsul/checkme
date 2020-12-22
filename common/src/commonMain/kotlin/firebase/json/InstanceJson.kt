package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class InstanceJson @JvmOverloads constructor(
        var done: Long? = null,
        var doneOffset: Double? = null,
        var instanceDate: String? = null,
        var instanceTime: String? = null,
        var hidden: Boolean = false,
        var parentJson: ParentJson? = null,
        var noParent: Boolean = false,
) {

    @Serializable
    data class ParentJson @JvmOverloads constructor(
            val taskId: String = "",
            val scheduleKey: String = "",
    )
}