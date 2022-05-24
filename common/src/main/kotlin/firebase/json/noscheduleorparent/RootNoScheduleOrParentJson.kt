package com.krystianwsul.common.firebase.json.noscheduleorparent

import kotlinx.serialization.Serializable

@Serializable
data class RootNoScheduleOrParentJson @JvmOverloads constructor(
    override val startTime: Long = 0,
    override val startTimeOffset: Double = 0.0,
    override var endTime: Long? = null,
    override var endTimeOffset: Double? = null,
    var projectId: String = "",
    var projectKey: String? = null, // todo projectKey check getters
) : NoScheduleOrParentJson