package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

class TaskHierarchyContainer<T : CustomTimeId, U : ProjectKey> {

    private val taskHierarchiesById = HashMap<String, TaskHierarchy<T, U>>()

    private val taskHierarchiesByParent = MultiMap<T, U>()
    private val taskHierarchiesByChild = MultiMap<T, U>()

    fun add(id: String, taskHierarchy: TaskHierarchy<T, U>) {
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

    fun getByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy<T, U>> = taskHierarchiesByChild.get(childTaskKey)

    fun getByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy<T, U>> = taskHierarchiesByParent.get(parentTaskKey)

    fun getById(id: String) = taskHierarchiesById[id]!!

    val all: Collection<TaskHierarchy<*, *>> get() = taskHierarchiesById.values

    private class MultiMap<T : CustomTimeId, U : ProjectKey> {

        private val values = mutableMapOf<TaskKey, MutableSet<TaskHierarchy<T, U>>>()

        fun put(taskKey: TaskKey, taskHierarchy: TaskHierarchy<T, U>): Boolean {
            if (!values.containsKey(taskKey))
                values[taskKey] = mutableSetOf()
            return values.getValue(taskKey).add(taskHierarchy)
        }

        fun containsEntry(
                taskKey: TaskKey,
                taskHierarchy: TaskHierarchy<T, U>
        ) = values[taskKey]?.contains(taskHierarchy) ?: false

        fun remove(
                taskKey: TaskKey,
                taskHierarchy: TaskHierarchy<T, U>
        ) = values[taskKey]?.remove(taskHierarchy) ?: false

        fun get(taskKey: TaskKey) = values[taskKey]?.toMutableSet() ?: mutableSetOf()
    }
}
