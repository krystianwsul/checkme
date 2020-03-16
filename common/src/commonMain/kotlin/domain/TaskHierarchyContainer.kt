package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.utils.TaskKey

class TaskHierarchyContainer<T, U : TaskHierarchy<*, *>> { // todo instance restructure these generics

    private val taskHierarchiesById = HashMap<T, U>()

    private val taskHierarchiesByParent = MultiMap<U>()
    private val taskHierarchiesByChild = MultiMap<U>()

    fun add(id: T, taskHierarchy: U) {
        check(!taskHierarchiesById.containsKey(id))

        taskHierarchiesById[id] = taskHierarchy
        check(taskHierarchiesByChild.put(taskHierarchy.childTaskKey, taskHierarchy))
        check(taskHierarchiesByParent.put(taskHierarchy.parentTaskKey, taskHierarchy))
    }

    fun removeForce(id: T) {
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

    fun getByChildTaskKey(childTaskKey: TaskKey): Set<U> = taskHierarchiesByChild.get(childTaskKey)

    fun getByParentTaskKey(parentTaskKey: TaskKey): Set<U> = taskHierarchiesByParent.get(parentTaskKey)

    fun getById(id: T) = taskHierarchiesById[id]!!

    val all: Collection<TaskHierarchy<*, *>> get() = taskHierarchiesById.values

    private class MultiMap<U : TaskHierarchy<*, *>> {

        private val values = mutableMapOf<TaskKey, MutableSet<U>>()

        fun put(taskKey: TaskKey, taskHierarchy: U): Boolean {
            if (!values.containsKey(taskKey))
                values[taskKey] = mutableSetOf()
            return values.getValue(taskKey).add(taskHierarchy)
        }

        fun containsEntry(taskKey: TaskKey, taskHierarchy: U) = values[taskKey]?.contains(taskHierarchy)
                ?: false

        fun remove(taskKey: TaskKey, taskHierarchy: U) = values[taskKey]?.remove(taskHierarchy)
                ?: false

        fun get(taskKey: TaskKey) = values[taskKey]?.toMutableSet() ?: setOf<U>()
    }
}
