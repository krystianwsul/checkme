package com.krystianwsul.common.firebase.json

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class NoScheduleOrParentJson @JvmOverloads constructor( // todo dst
        val startTime: Long = 0,
        var endTime: Long? = null
)