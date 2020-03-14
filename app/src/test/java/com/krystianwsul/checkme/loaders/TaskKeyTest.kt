package com.krystianwsul.checkme.loaders

import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.common.utils.ProjectKey
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class TaskKeyTest {

    @Test
    fun testEquals() {
        val projectId = ProjectKey.Shared("asdf")

        val parentKey1 = CreateTaskViewModel.ParentKey.Project(projectId)
        val parentKey2 = CreateTaskViewModel.ParentKey.Project(projectId)

        Assert.assertTrue(parentKey1 == parentKey2)
    }
}