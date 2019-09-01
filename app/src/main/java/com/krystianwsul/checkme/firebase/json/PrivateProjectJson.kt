package com.krystianwsul.checkme.firebase.json

import com.krystianwsul.common.firebase.ProjectJson
import com.krystianwsul.common.firebase.TaskHierarchyJson
import com.krystianwsul.common.firebase.TaskJson

class PrivateProjectJson @JvmOverloads constructor(
        override var name: String = "",
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        override var tasks: MutableMap<String, TaskJson> = mutableMapOf(),
        override var taskHierarchies: MutableMap<String, TaskHierarchyJson> = mutableMapOf(),
        override var customTimes: MutableMap<String, PrivateCustomTimeJson> = mutableMapOf()) : ProjectJson