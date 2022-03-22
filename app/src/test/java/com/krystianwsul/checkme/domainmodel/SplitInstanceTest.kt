package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.extensions.splitInstance
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.hours
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SplitInstanceTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    private fun getDayInstanceDatas(now: ExactTimeStamp.Local, day: Int = 0) =
        domainFactory.getGroupListData(now, day, Preferences.TimeRange.DAY, false)
            .groupListDataWrapper
            .allInstanceDatas

    private fun getSingleScheduleData(date: Date, hour: Int, minute: Int) =
        listOf(ScheduleData.Single(date, TimePair(HourMinute(hour, minute))))

    @Test
    fun testSplitSingleInstance() {
        val date = Date(2021, 12, 25)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            getSingleScheduleData(date, 5, 0),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val child1TaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("child 1 task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        now += 1.hours

        val child2TaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("child 2 task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        val parentInstanceKey = getDayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertEquals(2, it.single().allChildren.size)

            it.single().instanceKey
        }

        now += 1.hours

        domainUpdater(now).splitInstance(
            DomainListenerManager.NotificationType.All,
            parentInstanceKey,
        ).blockingSubscribe()

        getDayInstanceDatas(now).let {
            assertEquals(
                setOf(child1TaskKey, child2TaskKey),
                it.map { it.taskKey }.toSet(),
            )
        }
    }

    @Test
    fun testSplitWeeklyInstance() {
        val date = Date(2021, 12, 25)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            listOf(ScheduleData.Weekly(setOf(date.dayOfWeek), TimePair(HourMinute(5, 0)), null, null, 1)),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val child1TaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("child 1 task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        now += 1.hours

        val child2TaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("child 2 task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        val parentInstanceKey = getDayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertEquals(2, it.single().allChildren.size)

            it.single().instanceKey
        }

        getDayInstanceDatas(now, 7).let {
            assertEquals(1, it.size)
            assertEquals(2, it.single().allChildren.size)

            it.single().instanceKey
        }

        now += 1.hours

        domainUpdater(now).splitInstance(
            DomainListenerManager.NotificationType.All,
            parentInstanceKey,
        ).blockingSubscribe()

        getDayInstanceDatas(now).let {
            assertEquals(
                setOf(child1TaskKey, child2TaskKey),
                it.map { it.taskKey }.toSet(),
            )
        }

        getDayInstanceDatas(now, 7).let {
            assertEquals(1, it.size)
            assertEquals(2, it.single().allChildren.size)

            it.single().instanceKey
        }
    }
}