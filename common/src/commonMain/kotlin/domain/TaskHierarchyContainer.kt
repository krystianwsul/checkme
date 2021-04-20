package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.ProjectTaskHierarchy
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey

class TaskHierarchyContainer<T : ProjectType> {

    private val taskHierarchiesById = HashMap<String, ProjectTaskHierarchy<T>>()

    private val taskHierarchiesByParent = MultiMap<T>()
    private val taskHierarchiesByChild = MultiMap<T>()

    fun add(id: String, taskHierarchy: ProjectTaskHierarchy<T>) {
        check(!taskHierarchiesById.containsKey(id))

        taskHierarchiesById[id] = taskHierarchy
        check(taskHierarchiesByChild.put(taskHierarchy.childTaskKey, taskHierarchy))
        check(taskHierarchiesByParent.put(taskHierarchy.parentTaskKey, taskHierarchy))
    }

    fun removeForce(id: String) {
        check(taskHierarchiesById.containsKey(id))

        val taskHierarchy = taskHierarchiesById[id]!!

        taskHierarchiesById.remove(id)

        val childTaskKey = taskHierarchy.childTaskKey
        check(taskHierarchiesByChild.containsEntry(childTaskKey, taskHierarchy))

        check(taskHierarchiesByChild.remove(childTaskKey, taskHierarchy))

        val parentTaskKey = taskHierarchy.parentTaskKey
        check(taskHierarchiesByParent.containsEntry(parentTaskKey, taskHierarchy))

        check(taskHierarchiesByParent.remove(parentTaskKey, taskHierarchy))
    }

    fun getByChildTaskKey(childTaskKey: TaskKey): Set<ProjectTaskHierarchy<T>> = taskHierarchiesByChild.get(childTaskKey)

    fun getByParentTaskKey(parentTaskKey: TaskKey): Set<ProjectTaskHierarchy<T>> = taskHierarchiesByParent.get(parentTaskKey)

    fun getById(id: String) = taskHierarchiesById[id]!!

    val all: Collection<ProjectTaskHierarchy<T>> get() = taskHierarchiesById.values

    private class MultiMap<T : ProjectType> {

        private val values = mutableMapOf<TaskKey, MutableSet<ProjectTaskHierarchy<T>>>()

        fun put(taskKey: TaskKey, taskHierarchy: ProjectTaskHierarchy<T>): Boolean {
            if (!values.containsKey(taskKey))
                values[taskKey] = mutableSetOf()
            return values.getValue(taskKey).add(taskHierarchy)
        }

        fun containsEntry(
                taskKey: TaskKey,
                taskHierarchy: ProjectTaskHierarchy<T>,
        ) = values[taskKey]?.contains(taskHierarchy) ?: false

        fun remove(
                taskKey: TaskKey,
                taskHierarchy: ProjectTaskHierarchy<T>,
        ) = values[taskKey]?.remove(taskHierarchy) ?: false

        fun get(taskKey: TaskKey) = values[taskKey]?.toMutableSet() ?: mutableSetOf()
    }
}
