package com.krystianwsul.common.firebase.models.users

import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.*
import com.soywiz.klock.hours
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectOrdinalManagerMatcherTest {

    companion object {

        private fun newKey(parentInstanceKey: InstanceKey?, vararg entries: Pair<InstanceKey, DateTimePair>) =
            newKey(entries.toList(), parentInstanceKey)

        private fun newKey(vararg entries: Pair<InstanceKey, DateTimePair>) = newKey(entries.toList(), null)

        private fun newKey(
            entries: List<Pair<InstanceKey, DateTimePair>>,
            parentInstanceKey: InstanceKey?,
        ) = ProjectOrdinalManager.Key(
            entries.map { ProjectOrdinalManager.Key.Entry(it.first, it.second) }.toSet(),
            parentInstanceKey,
        )
    }

    private lateinit var projectOrdinalManager: ProjectOrdinalManager

    private val projectKey = ProjectKey.Shared("projectKey")

    private val project = mockk<SharedOwnedProject> {
        val timePairSlot = slot<TimePair>()

        every { this@mockk.projectKey } returns this@ProjectOrdinalManagerMatcherTest.projectKey

        every { getTime(capture(timePairSlot)) } answers {
            mockk {
                every { getHourMinute(any()) } returns timePairSlot.captured.hourMinute!!
            }
        }
    }

    @Before
    fun before() {
        projectOrdinalManager = ProjectOrdinalManager({ }, projectKey, mutableListOf())
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

        val ordinal = Ordinal.ONE

        projectOrdinalManager.setOrdinal(project, key, ordinal, now)

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(project, key))
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

        val ordinal = Ordinal.ONE

        projectOrdinalManager.setOrdinal(project, originalKey, ordinal, now)

        val rescheduledTimePair = TimePair(3, 0)
        val rescheduledInstanceDateTimePair = DateTimePair(date, rescheduledTimePair)

        val rescheduledKey = newKey(
            instanceKey1 to rescheduledInstanceDateTimePair,
            instanceKey2 to rescheduledInstanceDateTimePair,
        )

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(project, rescheduledKey))
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

        val ordinal = Ordinal.ONE

        projectOrdinalManager.setOrdinal(project, firstKey, ordinal, now)

        val secondDate = Date(2022, 1, 4)

        val secondInstanceDateTimePair = DateTimePair(secondDate, timePair)

        val instanceKey3 = InstanceKey(TaskKey.Root("taskKey3"), secondDate, timePair)
        val instanceKey4 = InstanceKey(TaskKey.Root("taskKey4"), secondDate, timePair)

        val secondKey = newKey(
            instanceKey3 to secondInstanceDateTimePair,
            instanceKey4 to secondInstanceDateTimePair,
        )

        assertEquals(ordinal, projectOrdinalManager.getOrdinal(project, secondKey))
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

        val firstOrdinal = Ordinal.ONE

        // first, we set an ordinal for all three instances
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        projectOrdinalManager.setOrdinal(project, notDoneKey, firstOrdinal, now)

        // second, we simulate an instance being marked as done, and the ordinal then being changed
        now += 1.hours

        val doneKey = newKey(
            instanceKey1 to instanceDateTimePair,
            instanceKey2 to instanceDateTimePair,
        )

        val secondOrdinal = 2.toOrdinal()

        projectOrdinalManager.setOrdinal(project, doneKey, secondOrdinal, now)
        assertEquals(secondOrdinal, projectOrdinalManager.getOrdinal(project, doneKey))

        // finally, we mark the instance not done, and make sure the new ordinal is still in place
        now += 1.hours

        assertEquals(secondOrdinal, projectOrdinalManager.getOrdinal(project, notDoneKey))
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

        val earlierOrdinal = Ordinal.ONE

        projectOrdinalManager.setOrdinal(project, earlierKey, earlierOrdinal, now)

        // 2. reschedule instances
        now += 1.hours

        val laterTimePair = TimePair(3, 0)
        val laterInstanceDateTimePair = DateTimePair(date, laterTimePair)

        val laterNotDoneKey = newKey(
            instanceKey1 to laterInstanceDateTimePair,
            instanceKey2 to laterInstanceDateTimePair,
            instanceKey3 to laterInstanceDateTimePair,
        )

        assertEquals(earlierOrdinal, projectOrdinalManager.getOrdinal(project, laterNotDoneKey))

        // 3. mark instance done

        now += 1.hours

        val laterDoneKey = newKey(
            instanceKey1 to laterInstanceDateTimePair,
            instanceKey2 to laterInstanceDateTimePair,
        )

        assertEquals(earlierOrdinal, projectOrdinalManager.getOrdinal(project, laterDoneKey))

        // 4. set new ordinal

        val newerOrdinal = 2.toOrdinal()

        projectOrdinalManager.setOrdinal(project, laterDoneKey, newerOrdinal, now)
        assertEquals(newerOrdinal, projectOrdinalManager.getOrdinal(project, laterDoneKey))

        // 5. mark instance not done

        now += 1.hours

        assertEquals(newerOrdinal, projectOrdinalManager.getOrdinal(project, laterNotDoneKey))

        // 6. set new ordinal

        now += 1.hours

        val newestOrdinal = 3.toOrdinal()

        projectOrdinalManager.setOrdinal(project, laterNotDoneKey, newestOrdinal, now)
        assertEquals(newestOrdinal, projectOrdinalManager.getOrdinal(project, laterNotDoneKey))

        // 7. mark instance done again

        now += 1.hours

        assertEquals(newestOrdinal, projectOrdinalManager.getOrdinal(project, laterDoneKey))
    }

    @Test
    fun testInstanceProject() {
        val date = Date(2022, 6, 13)
        val timePair = TimePair(12, 0)
        val dateTimePair = DateTimePair(date, timePair)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val instanceKey1 = InstanceKey(TaskKey.Root("taskKey1"), date, timePair)
        val instanceKey2 = InstanceKey(TaskKey.Root("taskKey2"), date, timePair)

        val parentInstanceKey = InstanceKey(TaskKey.Root("parentTaskKey"), date, timePair)

        val originalJoinedPairsOrdinal = Ordinal(1)

        fun List<InstanceKey>.toPairs() = map { it to dateTimePair }.toTypedArray()

        val originalJoinedPairs = listOf(instanceKey1, instanceKey2).toPairs()

        // drop two top-level instances, give them an ordinal

        projectOrdinalManager.setOrdinal(project, newKey(*originalJoinedPairs), originalJoinedPairsOrdinal, now)

        assertEquals(originalJoinedPairsOrdinal, projectOrdinalManager.getOrdinal(project, newKey(*originalJoinedPairs)))

        // make sure ordinal carried over when they get joined

        assertEquals(
            originalJoinedPairsOrdinal,
            projectOrdinalManager.getOrdinal(project, newKey(parentInstanceKey, *originalJoinedPairs))
        )

        now += 1.hours

        val instanceKey3 = InstanceKey(TaskKey.Root("taskKey3"), date, timePair)
        val instanceKey4 = InstanceKey(TaskKey.Root("taskKey4"), date, timePair)

        val topLevelOrdinal = Ordinal(2)

        val topLevelPairs = listOf(instanceKey3, instanceKey4).toPairs()

        projectOrdinalManager.setOrdinal(project, newKey(*topLevelPairs), topLevelOrdinal, now)

        assertEquals(
            originalJoinedPairsOrdinal,
            projectOrdinalManager.getOrdinal(project, newKey(parentInstanceKey, *originalJoinedPairs)),
        )

        assertEquals(topLevelOrdinal, projectOrdinalManager.getOrdinal(project, newKey(*topLevelPairs)))

        // let's give the joined ones a new ordinal

        now += 1.hours

        val newJoinedPairsOrdinal = Ordinal(3)

        projectOrdinalManager.setOrdinal(project, newKey(*originalJoinedPairs), newJoinedPairsOrdinal, now)

        assertEquals(
            newJoinedPairsOrdinal,
            projectOrdinalManager.getOrdinal(project, newKey(parentInstanceKey, *originalJoinedPairs)),
        )

        assertEquals(topLevelOrdinal, projectOrdinalManager.getOrdinal(project, newKey(*topLevelPairs)))

        // now, let's add another joined instance, and check if the ordinals "hold"

        now += 1.hours

        val instanceKey5 = InstanceKey(TaskKey.Root("taskKey4"), date, timePair)

        val augmentedJoinedPairs = listOf(instanceKey1, instanceKey2, instanceKey5).toPairs()

        assertEquals(
            newJoinedPairsOrdinal,
            projectOrdinalManager.getOrdinal(project, newKey(parentInstanceKey, *augmentedJoinedPairs)),
        )

        assertEquals(topLevelOrdinal, projectOrdinalManager.getOrdinal(project, newKey(*topLevelPairs)))
    }
}