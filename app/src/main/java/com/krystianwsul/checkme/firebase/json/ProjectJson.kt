package com.krystianwsul.checkme.firebase.json

class ProjectJson(
        var name: String = "",
        val startTime: Long = 0,
        var endTime: Long? = null,
        var tasks: MutableMap<String, TaskJson> = mutableMapOf(),
        var taskHierarchies: MutableMap<String, TaskHierarchyJson> = mutableMapOf(),
        var customTimes: MutableMap<String, CustomTimeJson> = mutableMapOf(),
        var users: MutableMap<String, UserJson> = mutableMapOf())
