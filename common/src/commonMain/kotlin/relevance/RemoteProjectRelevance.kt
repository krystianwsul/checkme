package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.project.Project

class RemoteProjectRelevance(val project: Project<*>) {

    var relevant = false
        private set

    fun setRelevant() {
        relevant = true
    }
}
