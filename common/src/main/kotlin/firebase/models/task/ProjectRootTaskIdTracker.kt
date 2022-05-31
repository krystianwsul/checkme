package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.singleOrEmpty

class ProjectRootTaskIdTracker {

    companion object {

        var instance: ProjectRootTaskIdTracker? = null

        fun checkTracking() = checkNotNull(instance)

        private data class TaskData(val task: RootTask, val keysToOmit: Set<TaskKey.Root>)

        fun <T> trackRootTaskIds(
            getRootTasks: () -> Map<TaskKey.Root, RootTask>,
            getProjects: () -> Map<ProjectKey<*>, Project<*>>,
            rootTaskProvider: OwnedProject.RootTaskProvider,
            action: () -> T,
        ): T {
            check(instance == null)

            return try {
                instance = ProjectRootTaskIdTracker()

                fun Map<TaskKey.Root, RootTask>.getRootTaskDatas() = mapValues { (_, task) ->
                    TaskData(task, task.taskRecord.getDirectDependencyTaskKeys() + task.taskKey)
                }

                val rootTasksBefore = getRootTasks()
                val rootTaskDatasBefore = rootTasksBefore.getRootTaskDatas()
                val mapBefore = getProjectTaskMap(rootTasksBefore)
                val graphsBefore = createRootTaskIdGraphs(rootTasksBefore)

                val result = action()

                val rootTasksAfter = getRootTasks()
                val rootTaskDatasAfter = rootTasksAfter.getRootTaskDatas()
                val mapAfter = getProjectTaskMap(rootTasksAfter)
                val graphsAfter = createRootTaskIdGraphs(rootTasksAfter)

                fun getGraphBefore(taskKey: TaskKey.Root) = graphsBefore.filter { taskKey in it }
                    .singleOrEmpty()
                    ?: emptySet()

                fun getGraphAfter(taskKey: TaskKey.Root) = graphsAfter.single { taskKey in it }

                rootTaskDatasAfter.forEach { (taskKey, taskData) ->
                    val (task, keysToOmitAfter) = taskData

                    val taskKeysBefore = getGraphBefore(taskKey) - rootTaskDatasBefore[taskKey]?.keysToOmit.orEmpty()
                    val taskKeysAfter = getGraphAfter(taskKey) - keysToOmitAfter

                    if (taskKeysBefore == taskKeysAfter) return@forEach

                    val addedTaskKeys = taskKeysAfter - taskKeysBefore
                    val removedTaskKeys = taskKeysBefore - taskKeysAfter

                    val delegate = task.taskRecord.rootTaskParentDelegate

                    addedTaskKeys.forEach(delegate::addRootTaskKey)
                    removedTaskKeys.forEach(delegate::removeRootTaskKey)

                    rootTaskProvider.updateTaskRecord(taskKey, taskKeysAfter)
                }

                getProjects().forEach { (projectKey, project) ->
                    val taskKeysBefore = mapBefore.getOrElse(projectKey.key) { emptyList() }
                        .map { task -> getGraphBefore(task.taskKey) }
                        .flatten()
                        .toSet()

                    val taskKeysAfter = mapAfter.getOrElse(projectKey.key) { emptyList() }
                        .map { task -> getGraphAfter(task.taskKey) }
                        .flatten()
                        .toSet()

                    if (taskKeysBefore == taskKeysAfter) return@forEach

                    val addedTaskKeys = taskKeysAfter - taskKeysBefore
                    val removedTaskKeys = taskKeysBefore - taskKeysAfter

                    val delegate = project.projectRecord.rootTaskParentDelegate

                    addedTaskKeys.forEach(delegate::addRootTaskKey)
                    removedTaskKeys.forEach(delegate::removeRootTaskKey)

                    rootTaskProvider.updateProjectRecord(projectKey, taskKeysAfter)
                }

                checkNotNull(instance)

                result
            } finally {
                instance = null
            }
        }

        private fun getProjectTaskMap(rootTasks: Map<TaskKey.Root, RootTask>) =
            rootTasks.values.groupBy { it.getTopLevelTask().projectId } // don't use project key, since project may not be loaded

        private fun createRootTaskIdGraphs(rootTasks: Map<TaskKey.Root, RootTask>): List<Set<TaskKey.Root>> {
            val graphs = mutableListOf<MutableSet<TaskKey.Root>>()

            rootTasks.values.forEach { addTaskToGraphs(rootTasks, it, graphs) }

            return graphs
        }

        private fun addTaskToGraphs(
            rootTasks: Map<TaskKey.Root, RootTask>,
            task: RootTask,
            graphs: MutableList<MutableSet<TaskKey.Root>>,
        ) {
            if (graphs.filter { task.taskKey in it }.singleOrEmpty() != null) return

            val graph = mutableSetOf(task.taskKey)

            addDependentTasksToGraph(rootTasks, task, graphs, graph)

            graphs += graph
        }

        private fun addDependentTasksToGraph(
            rootTasks: Map<TaskKey.Root, RootTask>,
            task: RootTask,
            graphs: MutableList<MutableSet<TaskKey.Root>>,
            graph: MutableSet<TaskKey.Root>,
        ) {
            task.nestedParentTaskHierarchies
                .values
                .forEach { addTaskToGraph(rootTasks, it.parentTask as RootTask, graphs, graph) }

            task.existingInstances
                .values
                .forEach {
                    it.parentState
                        .let { it as? Instance.ParentState.Parent }
                        ?.parentInstanceKey
                        ?.taskKey
                        ?.let { it as? TaskKey.Root }
                        ?.let { addTaskToGraph(rootTasks, rootTasks.getValue(it), graphs, graph) }
                }
        }

        private fun addTaskToGraph(
            rootTasks: Map<TaskKey.Root, RootTask>,
            task: RootTask,
            graphs: MutableList<MutableSet<TaskKey.Root>>,
            currentGraph: MutableSet<TaskKey.Root>,
        ) {
            if (task.taskKey in currentGraph) return

            val previousGraph = graphs.filter { task.taskKey in it }.singleOrEmpty()
            if (previousGraph != null) {
                graphs -= previousGraph

                // this task is already in a different graph, so we have to merge them
                currentGraph += previousGraph
                return
            }

            currentGraph += task.taskKey

            addDependentTasksToGraph(rootTasks, task, graphs, currentGraph)
        }
    }
}