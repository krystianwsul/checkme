package com.krystianwsul.common.firebase

class TaskJson @JvmOverloads constructor(
        var name: String = "",
        val startTime: Long = 0,
        var endTime: Long? = null,
        var oldestVisibleYear: Int? = null,
        var oldestVisibleMonth: Int? = null,
        var oldestVisibleDay: Int? = null,
        var note: String? = null,
        var instances: MutableMap<String, InstanceJson> = mutableMapOf(),
        var schedules: MutableMap<String, ScheduleWrapper> = mutableMapOf(),
        val oldestVisible: MutableMap<String, OldestVisibleJson> = mutableMapOf(),
        var image: Image? = null,
        var endData: EndData? = null) {

    data class Image(
            val imageUuid: String = "",
            val uploaderUuid: String? = null)

    data class EndData(
            val time: Long = 0,
            val deleteInstances: Boolean = false)
}