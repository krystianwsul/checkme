package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class RootInstanceRecord<T : ProjectType>(
        create: Boolean,
        taskRecord: TaskRecord<T>,
        createObject: InstanceJson,
        scheduleKey: ScheduleKey,
        firebaseKey: String,
        scheduleCustomTimeId: CustomTimeId<T>?,
        private val parent: Parent
) : InstanceRecord<T>(
        create,
        taskRecord,
        createObject,
        scheduleKey,
        "${taskRecord.projectId.key}-${taskRecord.id}-$firebaseKey",
        scheduleCustomTimeId
) {

    override fun deleteFromParent() = parent.removeRootInstanceRecord(instanceKey)

    interface Parent {

        fun removeRootInstanceRecord(instanceKey: InstanceKey)
    }
}