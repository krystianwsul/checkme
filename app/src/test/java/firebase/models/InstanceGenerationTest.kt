package firebase.models

import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.PrivateTaskJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.json.schedule.PrivateScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.PrivateSingleScheduleJson
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.PrivateTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectType
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

        private fun getOffset(hours: Int) = hours * 60 * 60 * 1000.0

        private val date = Date(2021, 4, 14)

        private val instanceHourMinute = HourMinute(12, 30)

        private const val localOffsetHours = 2
        private const val differentOffsetHours = 5
    }

    private fun createMockTask(hours: Int): Task<ProjectType.Private> {
        val offsetDouble = getOffset(hours)

        // let's say we have a schedule that was created at 12:00, and deleted at 13:00.  It has an instance at 12:30
        val startHourMinute = HourMinute(12, 0)
        val startExactTimeStamp = ExactTimeStamp.Offset.fromDateTime(DateTime(date, startHourMinute), offsetDouble)

        val endHourMinute = HourMinute(13, 0)
        val endExactTimeStamp = ExactTimeStamp.Offset.fromDateTime(DateTime(date, endHourMinute), offsetDouble)

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

        return Task(
                mockk(relaxed = true),
                PrivateTaskRecord(
                        "taskKey",
                        mockk(relaxed = true),
                        taskJson,
                ),
                mockk(relaxed = true),
        )
    }

    private fun testInstanceCorrectlyGeneratedForOffset(hours: Int) {
        val task = createMockTask(hours)

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

    @Test
    fun testInstanceCorrectlyGeneratedForLocal() {
        testInstanceCorrectlyGeneratedForOffset(localOffsetHours)
    }

    @Test
    fun testInstanceCorrectlyGeneratedForDifferentOffset() {
        testInstanceCorrectlyGeneratedForOffset(differentOffsetHours)
    }

    private fun testInstanceIsInPastInstancesWhenOneMinuteLaterForOffset(hours: Int) {
        val task = createMockTask(hours)

        val nowHourMinute = HourMinute(12, 31)

        // not actually relevant to test, just need a value
        val now = ExactTimeStamp.Local(date, nowHourMinute)
        val endExactTimeStamp = ExactTimeStamp.Offset.fromDateTime(
                DateTime(date, nowHourMinute),
                getOffset(hours),
        )

        assertEquals(
                DateTime(date, instanceHourMinute),
                task.getInstances(
                        null,
                        endExactTimeStamp.plusOne(), // offset 2
                        now,
                ).single().instanceDateTime,
        )
    }

    @Test
    fun testInstanceIsInPastInstancesWhenOneMinuteLaterForLocal() {
        testInstanceIsInPastInstancesWhenOneMinuteLaterForOffset(localOffsetHours)
    }

    @Test
    fun testInstanceIsInPastInstancesWhenOneMinuteLaterForDifferentOffset() {
        testInstanceIsInPastInstancesWhenOneMinuteLaterForOffset(differentOffsetHours)
    }
}