package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class RootNoScheduleOrParentJson @JvmOverloads constructor(
    override val startTime: Long = 0,
    override var startTimeOffset: Double? = null, // this is nullable only for project tasks
    override var endTime: Long? = null,
    override var endTimeOffset: Double? = null,
    override var projectId: String? = null, // this is nullable only for project tasks
) : NoScheduleOrParentJson