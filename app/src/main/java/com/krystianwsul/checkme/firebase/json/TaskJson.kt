package com.krystianwsul.checkme.firebase.json

class TaskJson(
        var name: String = "",
        val startTime: Long = 0,
        var endTime: Long? = null,
        var oldestVisibleYear: Int? = null,
        var oldestVisibleMonth: Int? = null,
        var oldestVisibleDay: Int? = null,
        var note: String? = null,
        var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
        var schedules: MutableMap<String, ScheduleWrapper> = mutableMapOf())