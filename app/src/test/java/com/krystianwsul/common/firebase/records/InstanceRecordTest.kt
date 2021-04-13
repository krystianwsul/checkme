package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.*
import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceRecordTest {

    private val projectKey = ProjectKey.Private("projectKey")

    private val provider = object : JsonTime.ProjectCustomTimeIdAndKeyProvider<ProjectType.Private> {

        override fun getProjectCustomTimeId(id: String) = CustomTimeId.Project.Private(id)

        override fun getProjectCustomTimeKey(projectCustomTimeId: CustomTimeId.Project<ProjectType.Private>) =
                CustomTimeKey.Project.Private(projectKey, projectCustomTimeId as CustomTimeId.Project.Private)
    }

    @Test
    fun testParsingScheduleKey() {
        // 2021-4-12-17-0

        val inputScheduleKey = ScheduleKey(Date(2021, 4, 12), TimePair(HourMinute(17, 0)))

        val scheduleKeyString = InstanceRecord.scheduleKeyToString(inputScheduleKey)
        assertEquals("2021-4-12-17-0", scheduleKeyString)

        val outputScheduleKey = InstanceRecord.stringToScheduleKey<ProjectType.Private>(provider, scheduleKeyString)
        assertEquals(inputScheduleKey, outputScheduleKey)
    }
}