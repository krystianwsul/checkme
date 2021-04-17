package com.krystianwsul.common.relevance

import com.krystianwsul.common.time.Time

class ProjectCustomTimeRelevance(val customTime: Time.Custom.Project<*>) { // todo customtime relevance

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
