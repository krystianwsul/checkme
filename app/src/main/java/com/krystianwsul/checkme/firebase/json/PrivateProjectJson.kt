package com.krystianwsul.checkme.firebase.json

class PrivateProjectJson @JvmOverloads constructor(
        override var name: String = "",
        override val startTime: Long = 0,
        override var endTime: Long? = null,
        override var tasks: MutableMap<String, TaskJson> = mutableMapOf(),
        override var taskHierarchies: MutableMap<String, TaskHierarchyJson> = mutableMapOf(),
        override var customTimes: MutableMap<String, PrivateCustomTimeJson> = mutableMapOf(),
        override var users: MutableMap<String, UserJson> = mutableMapOf()) : ProjectJson