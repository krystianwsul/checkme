package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectOrdinalManagerSerializationTest {

    @Test
    fun testDateTimePairHourMinuteSerialization() {
        val inputDateTimePair = DateTimePair(Date(2022, 1, 2), TimePair(HourMinute(20, 55)))
        val dateTimePairJson = inputDateTimePair.toJson()
        val outputDateTimePair = DateTimePair.fromJson(mockk(), dateTimePairJson)

        assertEquals(inputDateTimePair, outputDateTimePair)
    }

    @Test
    fun testTaskInfoRootTaskKeySerialization() {
        val taskKey = TaskKey.Root("taskKey")

        val inputTaskInfo = ProjectOrdinalManager.Key.TaskInfo(taskKey, null)
        val taskInfoJson = inputTaskInfo.toJson()
        val outputTaskInfo = ProjectOrdinalManager.Key.TaskInfo.fromJson(mockk(), taskInfoJson)

        assertEquals(taskKey, outputTaskInfo.taskKey)
    }

    @Test
    fun testTaskInfoProjectTaskKeySerialization() {
        val taskKey = TaskKey.Project(ProjectKey.Private("projectKey"), "taskKey")

        val inputTaskInfo = ProjectOrdinalManager.Key.TaskInfo(taskKey, null)
        val taskInfoJson = inputTaskInfo.toJson()
        val outputTaskInfo = ProjectOrdinalManager.Key.TaskInfo.fromJson(mockk(), taskInfoJson)

        assertEquals(taskKey, outputTaskInfo.taskKey)
    }

    @Test
    fun testDateSerialization() {
        val date = Date(2022, 1, 3)

        val inputDateOrDayOfWeek = DateOrDayOfWeek.Date(date)
        val dateOrDayOfWeekJson = inputDateOrDayOfWeek.toJson()
        val outputDateOrDayOfWeek = DateOrDayOfWeek.fromJson(dateOrDayOfWeekJson)

        assertEquals(
            date,
            outputDateOrDayOfWeek.let { it as DateOrDayOfWeek.Date }.date,
        )
    }

    @Test
    fun testDayOfWeekSerialization() {
        val dayOfWeek = DayOfWeek.WEDNESDAY

        val inputDateOrDayOfWeek = DateOrDayOfWeek.DayOfWeek(dayOfWeek)
        val dateOrDayOfWeekJson = inputDateOrDayOfWeek.toJson()
        val outputDateOrDayOfWeek = DateOrDayOfWeek.fromJson(dateOrDayOfWeekJson)

        assertEquals(
            dayOfWeek,
            outputDateOrDayOfWeek.let { it as DateOrDayOfWeek.DayOfWeek }.dayOfWeek,
        )
    }

    @Test
    fun testParentInstanceKeySerialization() {
        val instanceKey = InstanceKey(
            TaskKey.Root("taskKey"),
            InstanceScheduleKey(Date(2022, 6, 13), TimePair(1, 0)),
        )

        val key = ProjectOrdinalManager.Key(emptySet(), instanceKey)

        val entry =
            ProjectOrdinalManager.OrdinalEntry(key, ProjectOrdinalManager.Value(Ordinal.ZERO, ExactTimeStamp.Local.now))

        assertEquals(
            key,
            ProjectOrdinalManager.OrdinalEntry.fromJson(mockk(), entry.toJson()).key,
        )
    }
}