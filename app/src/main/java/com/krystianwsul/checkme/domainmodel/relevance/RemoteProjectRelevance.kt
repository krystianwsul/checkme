package com.krystianwsul.checkme.domainmodel.relevance

import com.krystianwsul.checkme.firebase.models.RemoteProject

class RemoteProjectRelevance(val remoteProject: RemoteProject<*>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
