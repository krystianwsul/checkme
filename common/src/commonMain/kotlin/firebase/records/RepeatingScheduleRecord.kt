package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.json.RepeatingScheduleJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.utils.ProjectType

abstract class RepeatingScheduleRecord<T : ProjectType>(
        taskRecord: TaskRecord<T>,
        createObject: ScheduleWrapper,
        repeatingScheduleJson: RepeatingScheduleJson,
        endTimeKey: String,
        id: String?
) : ScheduleRecord<T>(
        taskRecord,
        createObject,
        repeatingScheduleJson,
        endTimeKey,
        id
) {

    val from = repeatingScheduleJson.from
    val until = repeatingScheduleJson.until
}