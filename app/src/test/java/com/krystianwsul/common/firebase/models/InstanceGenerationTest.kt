package com.krystianwsul.common.firebase.models

import arrow.core.extensions.sequence.foldable.isEmpty
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.schedule.PrivateScheduleWrapper
import com.krystianwsul.common.firebase.json.schedule.PrivateWeeklyScheduleJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.models.task.ProjectTask
import com.krystianwsul.common.firebase.records.task.PrivateTaskRecord
import com.krystianwsul.common.time.*
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun createMockTask(hours: Int): ProjectTask {
        val offsetDouble = getOffset(hours)

        // let's say we have a schedule that was created at 12:00, and deleted at 13:00.  It has an instance at 12:30
        val startHourMinute = HourMinute(12, 0)
        val startExactTimeStamp = ExactTimeStamp.Offset.fromDateTime(DateTime(date, startHourMinute), offsetDouble)
        assertEquals(startHourMinute.toHourMilli(), startExactTimeStamp.hourMilli)

        val endHourMinute = HourMinute(13, 0)
        val endExactTimeStamp = ExactTimeStamp.Offset.fromDateTime(DateTime(date, endHourMinute), offsetDouble)
        assertEquals(endHourMinute.toHourMilli(), endExactTimeStamp.hourMilli)

        val weeklyScheduleJson = PrivateWeeklyScheduleJson(
                startTime = startExactTimeStamp.long,
                startTimeOffset = startExactTimeStamp.offset,
                endTime = endExactTimeStamp.long,
                endTimeOffset = endExactTimeStamp.offset,
                dayOfWeek = date.dayOfWeek.ordinal,
                hour = 12,
                minute = 30,
        )

        val taskJson = PrivateTaskJson(
                name = "task",
                startTime = startExactTimeStamp.long,
                startTimeOffset = startExactTimeStamp.offset,
                endData = TaskJson.EndData(endExactTimeStamp.long, endExactTimeStamp.offset),
                schedules = mutableMapOf("scheduleKey" to PrivateScheduleWrapper(weeklyScheduleJson = weeklyScheduleJson)),
        )

        return ProjectTask(
                mockk(relaxed = true),
                PrivateTaskRecord(
                        "taskKey",
                        mockk(relaxed = true),
                        taskJson,
                ),
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
                        endExactTimeStamp.plusOne(),
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

    private fun testInstanceNotInPastInstancesWhenOneMinuteEarlierForOffset(hours: Int) {
        val task = createMockTask(hours)

        val nowHourMinute = HourMinute(12, 29)

        // not actually relevant to test, just need a value
        val now = ExactTimeStamp.Local(date, nowHourMinute)
        val endExactTimeStamp = ExactTimeStamp.Offset.fromDateTime(
                DateTime(date, nowHourMinute),
                getOffset(hours),
        )

        assertTrue(
                task.getInstances(
                        null,
                        endExactTimeStamp.plusOne(),
                        now,
                ).isEmpty(),
        )
    }

    @Test
    fun testInstanceNotInPastInstancesWhenOneMinuteEarlierForLocal() {
        testInstanceNotInPastInstancesWhenOneMinuteEarlierForOffset(localOffsetHours)
    }

    @Test
    fun testInstanceNotInPastInstancesWhenOneMinuteEarlierForDifferentOffset() {
        testInstanceNotInPastInstancesWhenOneMinuteEarlierForOffset(differentOffsetHours)
    }
}