package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.firebase.RemoteProject

class RemoteProjectRelevance(val remoteProject: RemoteProject) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
