package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class LoadDependencyTrackerManager {

    private val trackerMap = mutableMapOf<ProjectKey<*>, ProjectTracker>()

    fun startTrackingProjectLoad(projectKey: ProjectKey<*>): ProjectTracker {
        check(!trackerMap.containsKey(projectKey))

        return trackerMap.getOrPut(projectKey) { ProjectTracker(this, projectKey) }
    }

    private fun stopTrackingProjectLoad(projectTracker: ProjectTracker) {
        check(trackerMap.remove(projectTracker.projectKey) == projectTracker)
    }

    fun isTaskKeyTracked(taskKey: TaskKey.Root) = trackerMap.values.any { it.taskKeys.contains(taskKey) }

    class ProjectTracker(
            private val loadDependencyTrackerManager: LoadDependencyTrackerManager,
            val projectKey: ProjectKey<*>,
    ) {

        val taskKeys = mutableSetOf<TaskKey.Root>()

        fun addTaskKey(taskKey: TaskKey.Root) {
            taskKeys += taskKey
        }

        fun stopTracking() = loadDependencyTrackerManager.stopTrackingProjectLoad(this)
    }
}