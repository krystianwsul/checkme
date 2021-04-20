package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.NoScheduleOrParentJson
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.utils.ProjectType

class NoScheduleOrParentRecord<T : ProjectType>(
        private val taskRecord: TaskRecord<T>,
        override val createObject: NoScheduleOrParentJson,
        _id: String?
) : RemoteRecord(_id == null) {

    companion object {

        const val NO_SCHEDULE_OR_PARENT = "noScheduleOrParent"
    }

    val id = _id ?: taskRecord.newNoScheduleOrParentRecordId()

    override val key = "${taskRecord.key}/$NO_SCHEDULE_OR_PARENT/$id"

    val startTime = createObject.startTime
    var startTimeOffset by Committer(createObject::startTimeOffset)

    var endTime by Committer(createObject::endTime)
    var endTimeOffset by Committer(createObject::endTimeOffset)

    override fun deleteFromParent() = check(taskRecord.noScheduleOrParentRecords.remove(id) == this)
}