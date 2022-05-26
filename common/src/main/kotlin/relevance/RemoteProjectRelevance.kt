package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.project.OwnedProject

class RemoteProjectRelevance(val project: OwnedProject<*>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
