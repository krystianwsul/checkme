package com.krystianwsul.checkme.utils

import com.google.common.collect.HashMultimap
import com.krystianwsul.checkme.domainmodel.TaskHierarchy
import junit.framework.Assert
import java.util.*

class TaskHierarchyContainer<T, U : TaskHierarchy> {

    private val taskHierarchiesById = HashMap<T, U>()

    private val taskHierarchiesByParent = HashMultimap.create<TaskKey, U>()

    private val taskHierarchiesByChild = HashMultimap.create<TaskKey, U>()

    fun add(id: T, taskHierarchy: U) {
        Assert.assertTrue(!taskHierarchiesById.containsKey(id))

        taskHierarchiesById[id] = taskHierarchy
        Assert.assertTrue(taskHierarchiesByChild.put(taskHierarchy.childTaskKey, taskHierarchy))
        Assert.assertTrue(taskHierarchiesByParent.put(taskHierarchy.parentTaskKey, taskHierarchy))
    }

    fun removeForce(id: T) {
        Assert.assertTrue(taskHierarchiesById.containsKey(id))

        val taskHierarchy = taskHierarchiesById[id]!!

        taskHierarchiesById.remove(id)

        val childTaskKey = taskHierarchy.childTaskKey
        Assert.assertTrue(taskHierarchiesByChild.containsEntry(childTaskKey, taskHierarchy))

        Assert.assertTrue(taskHierarchiesByChild.remove(childTaskKey, taskHierarchy))

        val parentTaskKey = taskHierarchy.parentTaskKey
        Assert.assertTrue(taskHierarchiesByParent.containsEntry(parentTaskKey, taskHierarchy))

        Assert.assertTrue(taskHierarchiesByParent.remove(parentTaskKey, taskHierarchy))
    }

    fun getByChildTaskKey(childTaskKey: TaskKey): Set<U> = taskHierarchiesByChild.get(childTaskKey)!!

    fun getByParentTaskKey(parentTaskKey: TaskKey): Set<U> = taskHierarchiesByParent.get(parentTaskKey)!!

    fun getById(id: T): U {
        Assert.assertTrue(taskHierarchiesById.containsKey(id))

        return taskHierarchiesById[id]!!
    }
}
