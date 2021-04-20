package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTest {

    @Test
    fun testHierarchyTimeStampWithFutureStart() {
        val date = Date(2020, 12, 11)
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0).toHourMilli()

        val now = ExactTimeStamp.Local(date, hour1)
        val start = ExactTimeStamp.Local(date, hour2)

        val task = Task(
                mockk(relaxed = true),
                mockk(relaxed = true) {
                    every { endData } returns null

                    every { startTime } returns start.long
                    every { startTimeOffset } returns null
                },
        )

        val hierarchyTimeStamp = task.getHierarchyExactTimeStamp(now)
        assertEquals(start.toOffset(), hierarchyTimeStamp)
        assertTrue(task.isTopLevelTask(hierarchyTimeStamp))
    }

    @Test
    fun testHierarchyTimeStampWithPastEnd() {
        val date = Date(2020, 12, 11)
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0).toHourMilli()

        val end = ExactTimeStamp.Local(date, hour1)
        val now = ExactTimeStamp.Local(date, hour2)

        val task = Task(
                mockk(relaxed = true),
                mockk(relaxed = true) {
                    every { endData } returns TaskJson.EndData(end.long)
                },
        )

        val hierarchyTimeStamp = task.getHierarchyExactTimeStamp(now)
        assertEquals(end.minusOne().toOffset(), hierarchyTimeStamp)
        assertTrue(task.isTopLevelTask(hierarchyTimeStamp))
    }
}