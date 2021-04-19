package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.schedule.RepeatingScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.ProjectType

abstract class RepeatingScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        createObject: ScheduleWrapper,
        repeatingScheduleJson: RepeatingScheduleJson,
        endTimeKey: String,
        id: String?,
) : ScheduleRecord<T>(
        taskRecord,
        createObject,
        repeatingScheduleJson,
        endTimeKey,
        id,
) {

    val from by lazy {
        repeatingScheduleJson.from?.let { Date.fromJson(it) }
    }

    val until by lazy {
        repeatingScheduleJson.until?.let { Date.fromJson(it) }
    }

    var oldestVisible by Committer(repeatingScheduleJson::oldestVisible, "$key/$endTimeKey")
}