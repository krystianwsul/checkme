package com.krystianwsul.common.firebase

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
class PrivateProjectJson @JvmOverloads constructor(
        override var name: String = "",
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        override var tasks: MutableMap<String, TaskJson> = mutableMapOf(),
        override var taskHierarchies: MutableMap<String, TaskHierarchyJson> = mutableMapOf(),
        override var customTimes: MutableMap<String, PrivateCustomTimeJson> = mutableMapOf()) : ProjectJson