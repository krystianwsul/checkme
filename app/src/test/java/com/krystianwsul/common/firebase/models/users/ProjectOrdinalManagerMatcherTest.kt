package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.invoke
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ProjectOrdinalManagerMatcherTest {

    companion object {

        private fun assertEquals(expected: Double, actual: Double) = Assert.assertEquals(expected, actual, 0.0)
    }

    private lateinit var projectOrdinalManager: ProjectOrdinalManager

    @Before
    fun before() {
        projectOrdinalManager = ProjectOrdinalManager(
            { _, timePair -> timePair.hourMinute!! },
            mockk(),
        )
    }

    @Test
    fun testSameKey() {
        val date = Date(2022, 1, 3)
        val timePair = TimePair(2, 3)

        val instanceKey1 = InstanceKey(TaskKey.Root("taskKey1"), date, timePair)
        val instanceKey2 = InstanceKey(TaskKey.Root("taskKey2"), date, timePair)

        val instanceDateTimePair = DateTimePair(date, timePair)

        val key = ProjectOrdinalManager.Key(
            setOf(
                ProjectOrdinalManager.Key.Entry(instanceKey1, instanceDateTimePair),
                ProjectOrdinalManager.Key.Entry(instanceKey2, instanceDateTimePair),
            )
        )

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val ordinal = 1.0

        projectOrdinalManager.setOrdinal(key, ordinal, now)

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(key))
    }
}