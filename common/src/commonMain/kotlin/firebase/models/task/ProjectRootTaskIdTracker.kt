package com.krystianwsul.common.firebase.models.task

interface ProjectRootTaskIdTracker {

    companion object {

        var instance: ProjectRootTaskIdTracker? = null

        fun checkTracking() = checkNotNull(instance)
    }
}