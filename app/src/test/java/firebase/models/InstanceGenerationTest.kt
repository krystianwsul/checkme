package firebase.models

import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.PrivateTaskJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.json.schedule.PrivateScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.PrivateSingleScheduleJson
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.PrivateTaskRecord
import com.krystianwsul.common.time.*
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class InstanceGenerationTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    @Test
    fun testInstanceCorrectlyGeneratedForLocal() {
        // let's say we have a schedule that was created at 12:00, and deleted at 13:00.  It has an instance at 12:30
        val date = Date(2021, 4, 14)
        val startHourMinute = HourMinute(12, 0)
        val startExactTimeStamp = ExactTimeStamp.Local(date, startHourMinute)

        val endHourMinute = HourMinute(13, 0)
        val endExactTimeStamp = ExactTimeStamp.Local(date, endHourMinute)

        val instanceHourMinute = HourMinute(12, 30)

        val singleScheduleJson = PrivateSingleScheduleJson(
                startTime = startExactTimeStamp.long,
                startTimeOffset = startExactTimeStamp.offset,
                endTime = endExactTimeStamp.long,
                endTimeOffset = endExactTimeStamp.offset,
                year = 2021,
                month = 4,
                day = 14,
                hour = 12,
                minute = 30,
        )

        val taskJson = PrivateTaskJson(
                name = "task",
                startTime = startExactTimeStamp.long,
                startTimeOffset = startExactTimeStamp.offset,
                endData = TaskJson.EndData(endExactTimeStamp.long, endExactTimeStamp.offset),
                schedules = mutableMapOf("scheduleKey" to PrivateScheduleWrapper(singleScheduleJson = singleScheduleJson)),
        )

        val task = Task(
                mockk(relaxed = true),
                PrivateTaskRecord(
                        "taskKey",
                        mockk(relaxed = true),
                        taskJson,
                ),
                mockk(relaxed = true),
        )

        val now = ExactTimeStamp.Local(Date(2021, 4, 14), HourMilli(15, 0, 0, 0))

        assertEquals(
                DateTime(date, instanceHourMinute),
                task.getInstances(
                        null,
                        null,
                        now,
                ).single().instanceDateTime,
        )
    }
}