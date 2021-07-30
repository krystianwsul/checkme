package com.krystianwsul.common.firebase.records.noscheduleorparent

import com.krystianwsul.common.firebase.json.noscheduleorparent.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.models.ProjectIdOwner
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord

abstract class NoScheduleOrParentRecord(
    taskRecord: TaskRecord,
    private val noScheduleOrParentJson: NoScheduleOrParentJson,
    val id: String,
    create: Boolean,
) : RemoteRecord(create), ProjectIdOwner {

    companion object {

        const val NO_SCHEDULE_OR_PARENT = "noScheduleOrParent"
    }

    override val key = "${taskRecord.key}/$NO_SCHEDULE_OR_PARENT/$id"

    val startTime = noScheduleOrParentJson.startTime
    abstract val startTimeOffset: Double?

    var endTime by Committer(noScheduleOrParentJson::endTime)
    var endTimeOffset by Committer(noScheduleOrParentJson::endTimeOffset)
}