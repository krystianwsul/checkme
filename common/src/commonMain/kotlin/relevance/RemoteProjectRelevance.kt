package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.RemoteProject

class RemoteProjectRelevance(val remoteProject: RemoteProject<*, *>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
