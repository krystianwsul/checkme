package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.task.Task

class IntervalInfo(val task: Task, val intervals: List<Interval>) {

    val scheduleIntervals by lazy {
        intervals.mapNotNull { (it.type as? Type.Schedule)?.getScheduleIntervals(it) }.flatten()
    }
}