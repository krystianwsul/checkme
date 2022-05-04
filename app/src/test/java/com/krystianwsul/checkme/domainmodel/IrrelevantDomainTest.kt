package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.days
import com.soywiz.klock.hours
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class IrrelevantDomainTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    @Before
    fun before() {
        RootTasksFactory.allowDeletion = true
    }

    @After
    fun after() {
        RootTasksFactory.allowDeletion = false
    }

    private fun getTodayInstanceDatas(now: ExactTimeStamp.Local) =
        domainFactory.getGroupListData(now, 0, false, Preferences.ProjectFilter.All)
            .groupListDataWrapper
            .allInstanceDatas

    @Test
    fun testRemovingScheduleWithUntil() {
        val today = Date(2022, 2, 20)
        val tomorrow = today + 1.days

        var now = ExactTimeStamp.Local(today, HourMinute(1, 0))

        val taskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task"),
            listOf(
                ScheduleData.Weekly(setOf(DayOfWeek.SUNDAY), TimePair(HourMinute(2, 0)), null, today, 1),
                ScheduleData.Single(tomorrow, TimePair(HourMinute(2, 0))),
            ),
            null,
        )
            .blockingGet()
            .taskKey

        assertEquals(2, domainFactory.getTaskForce(taskKey).getInstances(null, null, now).count())

        now += 1.hours

        val instanceKey = getTodayInstanceDatas(now).single().instanceKey

        domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            instanceKey,
            true
        ).blockingSubscribe()

        now += 2.days

        val result = Notifier.setIrrelevant(domainFactory, now)

        assertTrue(result.irrelevantSchedules.isNotEmpty())
    }

    @Test
    fun testSingleInstanceChildIrrelevantWhenDone() {
        val today = Date(2022, 4, 6)
        var now = ExactTimeStamp.Local(today, HourMinute(1, 0))

        // first, create the parent-child relationship

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            listOf(ScheduleData.Weekly(setOf(today.dayOfWeek), TimePair(10, 0), null, null, 1)),
            null,
        )
            .blockingGet()
            .taskKey

        val parentInstanceKey = getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertTrue(it.single().allChildren.isEmpty())

            it.single().instanceKey
        }

        now += 1.hours

        val childTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Instance(parentInstanceKey),
            EditDelegate.CreateParameters("child task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertEquals(1, it.single().allChildren.size)
        }

        // check that it doesn't get garbage collected immediately

        assertTrue(Notifier.setIrrelevant(domainFactory, now).irrelevantTasks.isEmpty())

        // mark parent as done

        domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            parentInstanceKey,
            true,
        ).blockingSubscribe()

        // sanity check

        getTodayInstanceDatas(now).let {
            assertNotNull(it.single().done)
        }

        // jump to tomorrow, do sanity check again

        now += 1.days

        getTodayInstanceDatas(now).let {
            assertTrue(it.isEmpty())
        }

        Notifier.setIrrelevant(domainFactory, now).let {
            assertEquals(childTaskKey, it.irrelevantTasks.single().taskKey)
        }
    }

    @Test
    fun testSingleInstanceChildRelevantWhenNotDone() {
        val today = Date(2022, 4, 6)
        var now = ExactTimeStamp.Local(today, HourMinute(1, 0))

        // first, create the parent-child relationship

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            listOf(ScheduleData.Weekly(setOf(today.dayOfWeek), TimePair(10, 0), null, null, 1)),
            null,
        )
            .blockingGet()
            .taskKey

        val parentInstanceKey = getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertTrue(it.single().allChildren.isEmpty())

            it.single().instanceKey
        }

        now += 1.hours

        val childTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Instance(parentInstanceKey),
            EditDelegate.CreateParameters("child task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertEquals(1, it.single().allChildren.size)
        }

        // check that it doesn't get garbage collected immediately

        assertTrue(Notifier.setIrrelevant(domainFactory, now).irrelevantTasks.isEmpty())

        // jump to tomorrow, do sanity check again

        now += 1.days

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertEquals(1, it.single().allChildren.size)
        }

        Notifier.setIrrelevant(domainFactory, now).let {
            assertTrue(it.irrelevantTasks.isEmpty())
        }
    }
}