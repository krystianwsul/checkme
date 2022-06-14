package com.krystianwsul.common.firebase.json.projects

import com.krystianwsul.common.firebase.json.customtimes.SharedCustomTimeJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.SharedTaskJson
import com.krystianwsul.common.firebase.json.users.UserJson
import kotlinx.serialization.Serializable

@Serializable
data class SharedOwnedProjectJson @JvmOverloads constructor(
    override var name: String = "",
    override val startTime: Long = 0,
    override var startTimeOffset: Double? = null,
    var endTime: Long? = null,
    var endTimeOffset: Double? = null,
    override var tasks: MutableMap<String, SharedTaskJson> = mutableMapOf(), // todo can be removed as of 2022-06-14
    override var taskHierarchies: MutableMap<String, ProjectTaskHierarchyJson> = mutableMapOf(), // todo can be removed as of 2022-06-14
    override var customTimes: MutableMap<String, SharedCustomTimeJson> = mutableMapOf(), // todo can be removed as of 2022-06-14
    override var users: MutableMap<String, UserJson> = mutableMapOf(),
    override val rootTaskIds: MutableMap<String, Boolean> = mutableMapOf(),
) : OwnedProjectJson, SharedProjectJson