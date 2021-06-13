package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RootTaskDependencyStateContainerTest {

    private fun makeRecordMock(
        taskKey: TaskKey.Root,
        childTaskKeys: Set<TaskKey.Root> = emptySet(),
    ) = mockk<RootTaskRecord> {
        every { this@mockk.taskKey } returns taskKey
        every { getDependentTaskKeys() } returns childTaskKeys
    }

    lateinit var container: RootTaskDependencyStateContainer.Impl

    @Before
    fun before() {
        container = RootTaskDependencyStateContainer.Impl()
    }

    @Test
    fun testInitial() {
        assertTrue(container.stateHoldersByTaskKey.isEmpty())
    }

    @Test
    fun testLoadNoChildren() {
        val taskKey = TaskKey.Root("taskKey")

        container.onLoaded(makeRecordMock(taskKey))

        assertTrue(container.hasDependentTasks(taskKey))
    }

    @Test
    fun testLoadOneChildAbsent() {
        val taskKey1 = TaskKey.Root("taskKey1")
        val taskKey2 = TaskKey.Root("taskKey2")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2)))
        assertFalse(container.hasDependentTasks(taskKey1))
    }

    @Test
    fun testLoadOneChildLoaded() {
        val taskKey1 = TaskKey.Root("taskKey1")
        val taskKey2 = TaskKey.Root("taskKey2")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2))
        assertTrue(container.hasDependentTasks(taskKey1))
    }

    @Test
    fun testLoadTwoChildren() {
        val taskKey1 = TaskKey.Root("taskKey1")
        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2, taskKey3)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey3))
        assertTrue(container.hasDependentTasks(taskKey1))
    }

    @Test
    fun testLoadTwoChildrenUpdate() {
        val taskKey1 = TaskKey.Root("taskKey1")
        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2, taskKey3)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2)))
        assertTrue(container.hasDependentTasks(taskKey1))
    }

    @Test
    fun testLoadTwoChildrenDouble() {
        val taskKey1 = TaskKey.Root("taskKey1")

        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")

        val taskKey4 = TaskKey.Root("taskKey4")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2, taskKey3)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey3, setOf(taskKey4)))
        assertFalse(container.hasDependentTasks(taskKey1))
        assertFalse(container.hasDependentTasks(taskKey3))

        container.onLoaded(makeRecordMock(taskKey4))
        assertTrue(container.hasDependentTasks(taskKey1))
        assertTrue(container.hasDependentTasks(taskKey3))
    }

    @Test
    fun testLoadTwoChildrenRemoved() {
        val taskKey1 = TaskKey.Root("taskKey1")

        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")

        val taskKey4 = TaskKey.Root("taskKey4")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2, taskKey3)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey3, setOf(taskKey4)))
        assertFalse(container.hasDependentTasks(taskKey1))
        assertFalse(container.hasDependentTasks(taskKey3))

        container.onLoaded(makeRecordMock(taskKey4))
        assertTrue(container.hasDependentTasks(taskKey1))
        assertTrue(container.hasDependentTasks(taskKey3))

        container.onRemoved(taskKey4)
        assertFalse(container.hasDependentTasks(taskKey1))
        assertFalse(container.hasDependentTasks(taskKey3))
    }

    @Test
    fun testLoadTwoChildrenUpdateRemoved() {
        val taskKey1 = TaskKey.Root("taskKey1")

        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")

        val taskKey4 = TaskKey.Root("taskKey4")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2, taskKey3)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey3, setOf(taskKey4)))
        assertFalse(container.hasDependentTasks(taskKey1))
        assertFalse(container.hasDependentTasks(taskKey3))

        container.onLoaded(makeRecordMock(taskKey4))
        assertTrue(container.hasDependentTasks(taskKey1))
        assertTrue(container.hasDependentTasks(taskKey3))

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2)))

        container.onRemoved(taskKey4)
        assertTrue(container.hasDependentTasks(taskKey1))
        assertFalse(container.hasDependentTasks(taskKey3))
    }

    @Test
    fun testCyclical() {
        val taskKey1 = TaskKey.Root("taskKey1")
        val taskKey2 = TaskKey.Root("taskKey2")
        val taskKey3 = TaskKey.Root("taskKey3")

        container.onLoaded(makeRecordMock(taskKey1, setOf(taskKey2)))
        assertFalse(container.hasDependentTasks(taskKey1))

        container.onLoaded(makeRecordMock(taskKey2, setOf(taskKey3)))
        assertFalse(container.hasDependentTasks(taskKey1))
        assertFalse(container.hasDependentTasks(taskKey2))

        container.onLoaded(makeRecordMock(taskKey3, setOf(taskKey1)))
        assertTrue(container.hasDependentTasks(taskKey1))
        assertTrue(container.hasDependentTasks(taskKey2))
        assertTrue(container.hasDependentTasks(taskKey3))

        // todo load add checks for REMOVING one in chain
    }
}