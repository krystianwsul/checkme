package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.utils.InstanceKey
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
    ) {

        constructor(instanceKey: InstanceKey) : this(
                instanceKey.taskKey.taskId,
                InstanceRecord.scheduleKeyToString(instanceKey.scheduleKey),
        )
    }
}