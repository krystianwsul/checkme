package com.krystianwsul.common.domain.schedules

import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.ScheduleId
import com.krystianwsul.common.utils.TaskKey

interface ScheduleBridge<T : ProjectType> {

    val startTime: Long

    var endTime: Long?

    val rootTaskKey: TaskKey

    fun delete()

    val customTimeKey: CustomTimeKey<T>?

    val hour: Int?

    val minute: Int?

    val scheduleId: ScheduleId

    val timePair: TimePair
}
