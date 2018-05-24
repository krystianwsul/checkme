package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime

class LocalCustomTimeRelevance(val localCustomTime: LocalCustomTime) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
