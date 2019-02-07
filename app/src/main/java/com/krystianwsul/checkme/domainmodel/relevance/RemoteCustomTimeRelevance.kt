package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.firebase.RemoteCustomTime

class RemoteCustomTimeRelevance(val remoteCustomTime: RemoteCustomTime<*>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
