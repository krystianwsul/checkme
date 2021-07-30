package com.krystianwsul.common.relevance

import com.krystianwsul.common.time.Time

class CustomTimeRelevance(val customTime: Time.Custom) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
