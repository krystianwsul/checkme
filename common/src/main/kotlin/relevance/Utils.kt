package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.schedule.Schedule
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ScheduleKey

fun MutableMap<InstanceKey, InstanceRelevance>.getOrPut(instance: Instance) =
    getOrPut(instance.instanceKey) { InstanceRelevance(instance) }

fun MutableMap<ScheduleKey, ScheduleRelevance>.getOrPut(schedule: Schedule) =
    getOrPut(schedule.key) { ScheduleRelevance(schedule) }