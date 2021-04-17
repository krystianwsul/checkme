package com.krystianwsul.common.relevance

import com.krystianwsul.common.time.Time

class UserCustomTimeRelevance(val customTime: Time.Custom.User) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
