package com.krystianwsul.common.firebase.json

import com.krystianwsul.common.firebase.json.customtimes.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.utils.ProjectType
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateProjectJson @JvmOverloads constructor(
        override var name: String = "",
        override val startTime: Long = 0,
        override var startTimeOffset: Double? = null,
        override var endTime: Long? = null,
        override var endTimeOffset: Double? = null,
        override var tasks: MutableMap<String, PrivateTaskJson> = mutableMapOf(),
        override var taskHierarchies: MutableMap<String, ProjectTaskHierarchyJson> = mutableMapOf(),
        override var customTimes: MutableMap<String, PrivateCustomTimeJson> = mutableMapOf(),
        var defaultTimesCreated: Boolean = false,
) : ProjectJson<ProjectType.Private>, Parsable