package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.singleOrEmpty

class ProjectRootTaskIdTracker {

    companion object {

        var instance: ProjectRootTaskIdTracker? = null

        fun checkTracking() = checkNotNull(instance)

        fun <T> trackRootTaskIds(
            rootTasks: Map<TaskKey.Root, RootTask>,
            projects: Map<ProjectKey<*>, Project<*>>,
            rootTaskProvider: Project.RootTaskProvider,
            action: () -> T,
        ): T {
            check(instance == null)

            instance = ProjectRootTaskIdTracker()

            val mapBefore = getProjectTaskMap(rootTasks)
            val graphsBefore = createRootTaskIdGraphs(rootTasks)

            val result = action()

            val mapAfter = getProjectTaskMap(rootTasks)
            val graphsAfter = createRootTaskIdGraphs(rootTasks)

            fun getGraphBefore(taskKey: TaskKey.Root) = graphsBefore.filter { taskKey in it }
                .singleOrEmpty()
                ?: emptySet()

            fun getGraphAfter(taskKey: TaskKey.Root) = graphsAfter.single { taskKey in it }

            rootTasks.forEach { (taskKey, task) ->
                val keysToOmit = task.taskRecord.getDirectDependencyTaskKeys() + task.taskKey

                val taskKeysBefore = getGraphBefore(taskKey) - keysToOmit
                val taskKeysAfter = getGraphAfter(taskKey) - keysToOmit

                if (taskKeysBefore == taskKeysAfter) return@forEach

                val addedTaskKeys = taskKeysAfter - taskKeysBefore
                val removedTaskKeys = taskKeysBefore - taskKeysAfter

                val delegate = task.taskRecord.rootTaskParentDelegate

                addedTaskKeys.forEach(delegate::addRootTaskKey)
                removedTaskKeys.forEach(delegate::removeRootTaskKey)

                rootTaskProvider.updateTaskRecord(taskKey, taskKeysAfter)
            }

            projects.forEach { (projectKey, project) ->
                val taskKeysBefore = mapBefore.getOrElse(projectKey) { emptyList() }
                    .map { task -> getGraphBefore(task.taskKey) }
                    .flatten()
                    .toSet()

                val taskKeysAfter = mapAfter.getOrElse(projectKey) { emptyList() }
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

            instance = null

            return result
        }

        private fun getProjectTaskMap(rootTasks: Map<TaskKey.Root, RootTask>) =
            rootTasks.values.groupBy { it.project.projectKey }

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