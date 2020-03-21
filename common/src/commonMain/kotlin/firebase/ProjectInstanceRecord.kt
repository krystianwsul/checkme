package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.records.InstanceRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleKey

class ProjectInstanceRecord<T : ProjectType>(
        create: Boolean,
        taskRecord: TaskRecord<T>,
        createObject: InstanceJson,
        scheduleKey: ScheduleKey,
        firebaseKey: String,
        scheduleCustomTimeId: CustomTimeId<T>?
) : InstanceRecord<T>(
        create,
        taskRecord,
        createObject,
        scheduleKey,
        taskRecord.key + "/instances/" + firebaseKey,
        scheduleCustomTimeId
) {

    override fun deleteFromParent() = check(taskRecord.remoteInstanceRecords.remove(scheduleKey) == this)
}