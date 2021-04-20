package com.krystianwsul.common.firebase.records.schedule

import com.krystianwsul.common.firebase.json.schedule.RepeatingScheduleJson
import com.krystianwsul.common.firebase.json.schedule.ScheduleWrapper
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.time.Date

abstract class RepeatingScheduleRecord(
        taskRecord: TaskRecord,
        createObject: ScheduleWrapper,
        repeatingScheduleJson: RepeatingScheduleJson,
        endTimeKey: String,
        id: String?,
) : ScheduleRecord(
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