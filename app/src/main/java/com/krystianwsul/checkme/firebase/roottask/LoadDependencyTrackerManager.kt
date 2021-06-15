package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.LoadStatus
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class LoadDependencyTrackerManager {

    private val projectTrackers = mutableMapOf<ProjectKey<*>, ProjectTracker>()
    private val taskTrackers = mutableMapOf<TaskKey.Root, TaskTracker>()

    private val allTrackers get() = projectTrackers.values + taskTrackers.values

    fun startTrackingProjectLoad(projectRecord: ProjectRecord<*>): ProjectTracker {
        val projectKey = projectRecord.projectKey
        check(!projectTrackers.containsKey(projectKey))

        val name = when (projectRecord) {
            is PrivateProjectRecord -> "private"
            is SharedProjectRecord -> projectRecord.name
        }

        return ProjectTracker(
            this,
            projectKey,
            name,
            projectRecord.rootTaskParentDelegate.rootTaskKeys,
        ).also { projectTrackers[projectKey] = it }
    }

    fun startTrackingTaskLoad(taskRecord: RootTaskRecord): TaskTracker {
        val taskKey = taskRecord.taskKey
        check(!taskTrackers.containsKey(taskKey))

        return TaskTracker(this, taskKey, taskRecord.name, taskRecord.getDependentTaskKeys()).also {
            taskTrackers[taskKey] = it
        }
    }

    private fun stopTrackingProjectLoad(projectTracker: ProjectTracker) {
        check(projectTrackers.remove(projectTracker.projectKey) == projectTracker)
    }

    private fun stopTrackingTaskLoad(taskTracker: TaskTracker) {
        check(taskTrackers.remove(taskTracker.taskKey) == taskTracker)
    }

    fun isTaskKeyTracked(taskKey: TaskKey.Root) = allTrackers.any { taskKey in it.dependentTaskKeys }

    interface Tracker {

        val dependentTaskKeys: Set<TaskKey.Root>
    }

    class ProjectTracker(
        private val loadDependencyTrackerManager: LoadDependencyTrackerManager,
        val projectKey: ProjectKey<*>,
        val projectName: String,
        override val dependentTaskKeys: Set<TaskKey.Root>,
    ) : Tracker {

        init {
            LoadStatus.incrementCounter()
        }

        fun stopTracking() {
            LoadStatus.decrementCounter()
            loadDependencyTrackerManager.stopTrackingProjectLoad(this)
        }
    }

    class TaskTracker(
        private val loadDependencyTrackerManager: LoadDependencyTrackerManager,
        val taskKey: TaskKey.Root,
        val taskName: String,
        override val dependentTaskKeys: Set<TaskKey.Root>,
    ) : Tracker {

        init {
            LoadStatus.incrementCounter()
        }

        fun stopTracking() {
            LoadStatus.decrementCounter()
            loadDependencyTrackerManager.stopTrackingTaskLoad(this)
        }
    }
}