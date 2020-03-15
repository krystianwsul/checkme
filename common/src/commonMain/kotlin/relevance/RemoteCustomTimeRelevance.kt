package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.CustomTime

class RemoteCustomTimeRelevance(val customTime: CustomTime<*, *>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
