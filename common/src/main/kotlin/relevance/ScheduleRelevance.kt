package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.schedule.Schedule

class ScheduleRelevance(val schedule: Schedule) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
