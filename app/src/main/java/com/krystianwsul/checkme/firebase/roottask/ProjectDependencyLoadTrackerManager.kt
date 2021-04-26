package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class ProjectDependencyLoadTrackerManager {

    private val trackerMap = mutableMapOf<ProjectKey<*>, Tracker>()

    fun startTrackingProjectLoad(projectKey: ProjectKey<*>): Tracker {
        check(!trackerMap.containsKey(projectKey))

        return trackerMap.getOrPut(projectKey) { Tracker(projectKey) }
    }

    fun stopTrackingProjectLoad(tracker: Tracker) {
        check(trackerMap.remove(tracker.projectKey) == tracker)
    }

    fun isTaskKeyTracked(taskKey: TaskKey.Root) = trackerMap.values.any { it.taskKeys.contains(taskKey) }

    class Tracker(val projectKey: ProjectKey<*>) {

        val taskKeys = mutableSetOf<TaskKey.Root>()

        fun addTaskKey(taskKey: TaskKey.Root) {
            taskKeys += taskKey
        }
    }
}