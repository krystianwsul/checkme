package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.utils.ProjectType
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class SharedProjectJson @JvmOverloads constructor(
        override var name: String = "",
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override var tasks: MutableMap<String, SharedTaskJson> = mutableMapOf(),
        override var taskHierarchies: MutableMap<String, TaskHierarchyJson> = mutableMapOf(),
        override var customTimes: MutableMap<String, SharedCustomTimeJson> = mutableMapOf(),
        var users: MutableMap<String, UserJson> = mutableMapOf(),
) : ProjectJson<ProjectType.Shared>