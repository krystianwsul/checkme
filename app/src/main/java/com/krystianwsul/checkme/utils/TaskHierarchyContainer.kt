package com.krystianwsul.checkme.utils

import com.google.common.collect.HashMultimap
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import com.krystianwsul.common.utils.TaskKey

import java.util.*

class TaskHierarchyContainer<T, U : TaskHierarchy> {

    private val taskHierarchiesById = HashMap<T, U>()

    private val taskHierarchiesByParent = HashMultimap.create<TaskKey, U>()

    private val taskHierarchiesByChild = HashMultimap.create<TaskKey, U>()

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

    fun getByChildTaskKey(childTaskKey: TaskKey): Set<U> = taskHierarchiesByChild.get(childTaskKey)!!

    fun getByParentTaskKey(parentTaskKey: TaskKey): Set<U> = taskHierarchiesByParent.get(parentTaskKey)!!

    fun getById(id: T) = taskHierarchiesById[id]!!

    val all: Collection<TaskHierarchy> get() = taskHierarchiesById.values
}
