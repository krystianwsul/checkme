package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.utils.TaskKey
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RootTaskDependencyStateContainerTest {

    private fun makeRecordMock(taskKey: TaskKey.Root, childTaskKeys: Set<TaskKey.Root>) = mockk<RootTaskRecord> {
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
}