package com.krystianwsul.common.relevance

import com.krystianwsul.common.time.Time

class RemoteCustomTimeRelevance(val customTime: Time.Custom.Project<*>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
