package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.taskhierarchy.ProjectTaskHierarchy
import com.krystianwsul.common.utils.TaskKey

class TaskHierarchyContainer {

    private val taskHierarchiesById = HashMap<String, ProjectTaskHierarchy>()

    private val taskHierarchiesByParent = MultiMap()
    private val taskHierarchiesByChild = MultiMap()

    fun add(id: String, taskHierarchy: ProjectTaskHierarchy) {
        check(!taskHierarchiesById.containsKey(id))

        taskHierarchiesById[id] = taskHierarchy
        check(taskHierarchiesByChild.put(taskHierarchy.childTaskKey, taskHierarchy))
        check(taskHierarchiesByParent.put(taskHierarchy.parentTaskKey as TaskKey.Project, taskHierarchy))
    }

    fun removeForce(id: String) {
        check(taskHierarchiesById.containsKey(id))

        val taskHierarchy = taskHierarchiesById[id]!!

        taskHierarchiesById.remove(id)

        val childTaskKey = taskHierarchy.childTaskKey
        check(taskHierarchiesByChild.containsEntry(childTaskKey, taskHierarchy))

        check(taskHierarchiesByChild.remove(childTaskKey, taskHierarchy))

        val parentTaskKey = taskHierarchy.parentTaskKey
        check(taskHierarchiesByParent.containsEntry(parentTaskKey as TaskKey.Project, taskHierarchy))

        check(taskHierarchiesByParent.remove(parentTaskKey, taskHierarchy))
    }

    fun getByChildTaskKey(childTaskKey: TaskKey.Project): Set<ProjectTaskHierarchy> =
            taskHierarchiesByChild.get(childTaskKey)

    fun getByParentTaskKey(parentTaskKey: TaskKey.Project): Set<ProjectTaskHierarchy> =
            taskHierarchiesByParent.get(parentTaskKey)

    fun getById(id: String) = taskHierarchiesById[id]!!

    val all: Collection<ProjectTaskHierarchy> get() = taskHierarchiesById.values

    private class MultiMap {

        private val values = mutableMapOf<TaskKey.Project, MutableSet<ProjectTaskHierarchy>>()

        fun put(taskKey: TaskKey.Project, taskHierarchy: ProjectTaskHierarchy): Boolean {
            if (!values.containsKey(taskKey))
                values[taskKey] = mutableSetOf()
            return values.getValue(taskKey).add(taskHierarchy)
        }

        fun containsEntry(
                taskKey: TaskKey.Project,
                taskHierarchy: ProjectTaskHierarchy,
        ) = values[taskKey]?.contains(taskHierarchy) ?: false

        fun remove(
                taskKey: TaskKey.Project,
                taskHierarchy: ProjectTaskHierarchy,
        ) = values[taskKey]?.remove(taskHierarchy) ?: false

        fun get(taskKey: TaskKey.Project) = values[taskKey]?.toMutableSet() ?: mutableSetOf()
    }
}
