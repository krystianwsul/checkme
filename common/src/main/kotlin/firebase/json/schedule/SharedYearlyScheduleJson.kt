package com.krystianwsul.common.firebase.json.schedule

import kotlinx.serialization.Serializable

@Serializable
data class SharedYearlyScheduleJson @JvmOverloads constructor(
    override val startTime: Long = 0,
    override var startTimeOffset: Double? = null,
    override var endTime: Long? = null,
    override var endTimeOffset: Double? = null,
    override val month: Int = 0,
    override val day: Int = 0,
    override val customTimeId: String? = null,
    override val hour: Int? = null,
    override val minute: Int? = null,
    override val from: String? = null,
    override val until: String? = null,
    override var oldestVisible: String? = null, // todo remove once all versions > 0.75.1
    override var oldestVisibleJson: String? = null,
    override val assignedTo: Map<String, Boolean> = mapOf(),
    override val time: String? = null,
) : YearlyScheduleJson, AssignedToJson, ProjectScheduleJson