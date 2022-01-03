package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.invoke
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.soywiz.klock.hours
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ProjectOrdinalManagerMatcherTest {

    companion object {

        private fun assertEquals(expected: Double, actual: Double) = Assert.assertEquals(expected, actual, 0.0)

        private fun newKey(vararg entries: Pair<InstanceKey, DateTimePair>) = ProjectOrdinalManager.Key(
            entries.map { ProjectOrdinalManager.Key.Entry(it.first, it.second) }.toSet()
        )
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

        val key = newKey(
            instanceKey1 to instanceDateTimePair,
            instanceKey2 to instanceDateTimePair,
        )

        val now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val ordinal = 1.0

        projectOrdinalManager.setOrdinal(key, ordinal, now)

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(key))
    }

    @Test
    fun testRescheduleInstances() {
        val date = Date(2022, 1, 3)
        val originalTimePair = TimePair(2, 0)

        val instanceKey1 = InstanceKey(TaskKey.Root("taskKey1"), date, originalTimePair)
        val instanceKey2 = InstanceKey(TaskKey.Root("taskKey2"), date, originalTimePair)

        val originalInstanceDateTimePair = DateTimePair(date, originalTimePair)

        val originalKey = newKey(
            instanceKey1 to originalInstanceDateTimePair,
            instanceKey2 to originalInstanceDateTimePair,
        )

        val now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val ordinal = 1.0

        projectOrdinalManager.setOrdinal(originalKey, ordinal, now)

        val rescheduledTimePair = TimePair(3, 0)
        val rescheduledInstanceDateTimePair = DateTimePair(date, rescheduledTimePair)

        val rescheduledKey = newKey(
            instanceKey1 to rescheduledInstanceDateTimePair,
            instanceKey2 to rescheduledInstanceDateTimePair,
        )

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(rescheduledKey))
    }

    @Test
    fun testHintOnOtherDay() {
        val firstDate = Date(2022, 1, 3)
        val timePair = TimePair(2, 0)

        val instanceKey1 = InstanceKey(TaskKey.Root("taskKey1"), firstDate, timePair)
        val instanceKey2 = InstanceKey(TaskKey.Root("taskKey2"), firstDate, timePair)

        val firstInstanceDateTimePair = DateTimePair(firstDate, timePair)

        val firstKey = newKey(
            instanceKey1 to firstInstanceDateTimePair,
            instanceKey2 to firstInstanceDateTimePair,
        )

        val now = ExactTimeStamp.Local(firstDate, HourMinute(1, 0))

        val ordinal = 1.0

        projectOrdinalManager.setOrdinal(firstKey, ordinal, now)

        val secondDate = Date(2022, 1, 4)

        val secondInstanceDateTimePair = DateTimePair(secondDate, timePair)

        val instanceKey3 = InstanceKey(TaskKey.Root("taskKey3"), secondDate, timePair)
        val instanceKey4 = InstanceKey(TaskKey.Root("taskKey4"), secondDate, timePair)

        val secondKey = newKey(
            instanceKey3 to secondInstanceDateTimePair,
            instanceKey4 to secondInstanceDateTimePair,
        )

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(secondKey))
    }

    @Test
    fun testSetOrdinalMarkDoneSetOrdinalMarkUndone() {
        val date = Date(2022, 1, 3)
        val timePair = TimePair(2, 3)

        val instanceKey1 = InstanceKey(TaskKey.Root("taskKey1"), date, timePair)
        val instanceKey2 = InstanceKey(TaskKey.Root("taskKey2"), date, timePair)
        val instanceKey3 = InstanceKey(TaskKey.Root("taskKey3"), date, timePair)

        val instanceDateTimePair = DateTimePair(date, timePair)

        val notDoneKey = newKey(
            instanceKey1 to instanceDateTimePair,
            instanceKey2 to instanceDateTimePair,
            instanceKey3 to instanceDateTimePair,
        )

        val firstOrdinal = 1.0

        // first, we set an ordinal for all three instances
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        projectOrdinalManager.setOrdinal(notDoneKey, firstOrdinal, now)

        // second, we simulate an instance being marked as done, and the ordinal then being changed
        now += 1.hours

        val doneKey = newKey(
            instanceKey1 to instanceDateTimePair,
            instanceKey2 to instanceDateTimePair,
        )

        val secondOrdinal = 2.0

        projectOrdinalManager.setOrdinal(doneKey, secondOrdinal, now)
        assertEquals(secondOrdinal, projectOrdinalManager.getOrdinal(doneKey))

        // finally, we mark the instance not done, and make sure the new ordinal is still in place
        now += 1.hours

        assertEquals(secondOrdinal, projectOrdinalManager.getOrdinal(notDoneKey))
    }

    @Test
    fun testRescheduleInstancesMarkDoneSetOrdinalMarkUndone() {
        val date = Date(2022, 1, 3)
        val earlierTimePair = TimePair(2, 0)

        val instanceKey1 = InstanceKey(TaskKey.Root("taskKey1"), date, earlierTimePair)
        val instanceKey2 = InstanceKey(TaskKey.Root("taskKey2"), date, earlierTimePair)
        val instanceKey3 = InstanceKey(TaskKey.Root("taskKey3"), date, earlierTimePair)

        val earlierInstanceDateTimePair = DateTimePair(date, earlierTimePair)

        val earlierKey = newKey(
            instanceKey1 to earlierInstanceDateTimePair,
            instanceKey2 to earlierInstanceDateTimePair,
            instanceKey3 to earlierInstanceDateTimePair,
        )

        // 1. set ordinal for earlier time
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val earlierOrdinal = 1.0

        projectOrdinalManager.setOrdinal(earlierKey, earlierOrdinal, now)

        // 2. reschedule instances
        now += 1.hours

        val laterTimePair = TimePair(3, 0)
        val laterInstanceDateTimePair = DateTimePair(date, laterTimePair)

        val laterNotDoneKey = newKey(
            instanceKey1 to laterInstanceDateTimePair,
            instanceKey2 to laterInstanceDateTimePair,
            instanceKey3 to laterInstanceDateTimePair,
        )

        assertEquals(earlierOrdinal, projectOrdinalManager.getOrdinal(laterNotDoneKey))

        // 3. mark instance done

        now += 1.hours

        val laterDoneKey = newKey(
            instanceKey1 to laterInstanceDateTimePair,
            instanceKey2 to laterInstanceDateTimePair,
        )

        assertEquals(earlierOrdinal, projectOrdinalManager.getOrdinal(laterDoneKey))

        // 4. set new ordinal

        val newerOrdinal = 2.0

        projectOrdinalManager.setOrdinal(laterDoneKey, newerOrdinal, now)
        assertEquals(newerOrdinal, projectOrdinalManager.getOrdinal(laterDoneKey))

        // 5. mark instance not done

        now += 1.hours

        assertEquals(newerOrdinal, projectOrdinalManager.getOrdinal(laterNotDoneKey))

        // 6. set new ordinal

        now += 1.hours

        val newestOrdinal = 3.0

        projectOrdinalManager.setOrdinal(laterNotDoneKey, newestOrdinal, now)
        assertEquals(newestOrdinal, projectOrdinalManager.getOrdinal(laterNotDoneKey))

        // 7. mark instance done again

        now += 1.hours

        assertEquals(newestOrdinal, projectOrdinalManager.getOrdinal(laterDoneKey))
    }
}