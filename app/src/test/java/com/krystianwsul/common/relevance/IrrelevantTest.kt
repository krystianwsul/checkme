package com.krystianwsul.common.relevance

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IrrelevantTest {

    companion object {

        private val userKey = UserKey("key")

        private val userInfo = spyk(UserInfo("email", "name", "uid")) {
            every { key } returns userKey
        }

        private val databaseWrapper = mockk<DatabaseWrapper> {
            every { getPrivateTaskRecordId(any()) } returns "taskRecordId"
            every { newPrivateNoScheduleOrParentRecordId(any(), any()) } returns "noScheduleOrParentRecordId"
        }
    }

    @Test
    fun testDisappearingTask() {
        // 1: create task with single schedule

        val day1 = Date(2020, 1, 1)
        val day2 = Date(2020, 1, 2)
        val hour1 = HourMinute(1, 1).toHourMilli()
        val hour2 = HourMinute(2, 1).toHourMilli()
        val hour3 = HourMinute(3, 1).toHourMilli()
        val hour4 = HourMinute(4, 1)

        val parent = mockk<Project.Parent>()

        var now = ExactTimeStamp(day1, hour1)

        val projectJson = PrivateProjectJson(startTime = now.long)
        val projectRecord = PrivateProjectRecord(databaseWrapper, userInfo, projectJson)

        val project = PrivateProject(projectRecord, mapOf()) {
            mockk {
                every { records } returns mutableListOf()
            }
        }

        now = ExactTimeStamp(day1, hour2)

        val scheduleWrapper = ScheduleWrapper(
                singleScheduleJson = SingleScheduleJson(
                        startTime = now.long,
                        year = day1.year,
                        month = day1.month,
                        day = day1.day,
                        hour = hour3.hour,
                        minute = hour3.minute
                )
        )

        val taskJson = TaskJson(
                name = "task",
                startTime = now.long,
                schedules = mutableMapOf("scheduleKey" to scheduleWrapper)
        )

        val task = project.newTask(taskJson)

        // 2: once reminded, add one hour

        now = ExactTimeStamp(day1, hour3)

        val instance = task.getPastRootInstances(now).single()

        val shownFactory = mockk<Instance.ShownFactory> {
            every { getShown(any(), any()) } returns mockk(relaxed = true)
        }

        instance.setInstanceDateTime(shownFactory, userKey, DateTime(day1, Time.Normal(hour4)), now)

        // 3: after second reminder, remove schedule, then set reminder done

        now = ExactTimeStamp(day1, hour4.toHourMilli())

        task.apply {
            endAllCurrentTaskHierarchies(now)
            endAllCurrentSchedules(now)
            endAllCurrentNoScheduleOrParents(now)

            setNoScheduleOrParent(now)
        }

        instance.setDone(shownFactory, true, now)

        fun Task<*>.isReminderless() = current(now) && isVisible(now, true) && isRootTask(now) && getCurrentSchedules(now).isEmpty()

        assertTrue(task.isReminderless())

        // 4: next day, task should still be reminderless, instead of ending up with expired schedule again

        now = ExactTimeStamp(day2, hour1)

        Irrelevant.setIrrelevant(parent, project, now)

        assertTrue(task.isReminderless())
    }

    @Test
    fun testSingleScheduleChangedToRepeating() {
        // 1. Create task with single schedule and repeating schedule

        val day1 = Date(2020, 10, 6) // tuesday
        val day2 = Date(2020, 10, 7) // wednesday
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0).toHourMilli()
        val hour3 = HourMinute(3, 0).toHourMilli()

        val parent = mockk<Project.Parent>()

        var now = ExactTimeStamp(day1, hour1)

        val singleScheduleWrapper = ScheduleWrapper(
                singleScheduleJson = SingleScheduleJson(
                        startTime = now.long,
                        year = day1.year,
                        month = day1.month,
                        day = day1.day,
                        hour = hour2.hour,
                        minute = hour2.minute
                )
        )

        val weeklyScheduleWrapper = ScheduleWrapper(
                weeklyScheduleJson = WeeklyScheduleJson(
                        startTime = now.long,
                        dayOfWeek = 1, // sunday
                        hour = hour1.hour,
                        minute = hour1.minute
                )
        )

        val taskJson = TaskJson(
                name = "task",
                startTime = now.long,
                schedules = mutableMapOf(
                        "singleScheduleKey" to singleScheduleWrapper,
                        "weeklyScheduleKey" to weeklyScheduleWrapper
                )
        )

        val projectKey = ProjectKey.Private(userKey.key)

        val taskId = "taskKey"
        val taskKey = TaskKey(ProjectKey.Private(userKey.key), taskId)

        val projectJson = PrivateProjectJson(
                startTime = now.long,
                tasks = mutableMapOf(taskId to taskJson)
        )
        val projectRecord = PrivateProjectRecord(databaseWrapper, projectKey, projectJson)

        val project = PrivateProject(
                projectRecord,
                mapOf(
                        taskKey to mockk {
                            every { records } returns mutableListOf()
                        }
                )
        ) {
            mockk {
                every { records } returns mutableListOf()
            }
        }

        val task = project.tasks.single()

        // 2. Mark single instance done

        assertTrue(task.getCurrentSchedules(now).size == 2)

        now = ExactTimeStamp(day1, hour2)

        val instance = task.getPastRootInstances(now).single()

        val shownFactory = mockk<Instance.ShownFactory> {
            every { getShown(any(), any()) } returns mockk(relaxed = true)
        }

        instance.setDone(shownFactory, true, now)
        projectRecord.getValues(mutableMapOf())

        // 3. Check both instance and schedule removed next day

        now = ExactTimeStamp(day2, hour3)

        assertFalse(task.getPastRootInstances(now).single().isVisible(now, true))
        assertTrue(task.getCurrentSchedules(now).size == 2)

        Irrelevant.setIrrelevant(parent, project, now)

        assertTrue(task.getCurrentSchedules(now).size == 1)
        assertTrue(task.getPastRootInstances(now).toList().isEmpty())
    }
}