package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.hours
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class MockHierarchyTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    private fun getTodayInstanceDatas(now: ExactTimeStamp.Local) =
        domainFactory.getGroupListData(now, 0, false, Preferences.ProjectFilter.All)
            .groupListDataWrapper
            .allInstanceDatas

    private fun getSingleScheduleData(date: Date, hour: Int, minute: Int) =
        listOf(ScheduleData.Single(date, TimePair(HourMinute(hour, minute))))

    @Ignore("todo join test")
    @Test
    fun testSetSingleScheduleForMockChildTask() {
        val date = Date(2022, 2, 2)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val singleScheduleData = getSingleScheduleData(date, 5, 0)

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            singleScheduleData,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val childTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task"),
            singleScheduleData,
            null,
        )
            .blockingGet()
            .taskKey

        val (parentInstanceKey, childInstanceKey) = getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)

            it.single { it.taskKey == parentTaskKey }.instanceKey to it.single { it.taskKey == childTaskKey }.instanceKey
        }

        now += 1.hours

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(parentInstanceKey, it.single().instanceKey)
            assertEquals(childInstanceKey, it.single().allChildren.single().instanceKey)
        }

        assertEquals(1, domainFactory.getTaskForce(childTaskKey).intervalInfo.getCurrentScheduleIntervals(now).size)
        assertEquals(parentTaskKey, domainFactory.getTaskForce(childTaskKey).parentTask?.taskKey)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().any { it.taskKey == childTaskKey })

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            EditDelegate.CreateParameters("child task"),
            singleScheduleData,
            null,
        ).blockingSubscribe()

        assertEquals(2, getTodayInstanceDatas(now).size)
        assertNull(domainFactory.getTaskForce(childTaskKey).parentTask)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().none { it.taskKey == childTaskKey })
    }

    @Ignore("todo join test")
    @Test
    fun testUpdateNameForMockChildTask() {
        val date = Date(2022, 2, 2)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val singleScheduleData = getSingleScheduleData(date, 5, 0)

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            singleScheduleData,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val childTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task"),
            singleScheduleData,
            null,
        )
            .blockingGet()
            .taskKey

        val (parentInstanceKey, childInstanceKey) = getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)

            it.single { it.taskKey == parentTaskKey }.instanceKey to it.single { it.taskKey == childTaskKey }.instanceKey
        }

        now += 1.hours

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(parentInstanceKey, it.single().instanceKey)
            assertEquals(childInstanceKey, it.single().allChildren.single().instanceKey)
        }

        assertEquals(1, domainFactory.getTaskForce(childTaskKey).intervalInfo.getCurrentScheduleIntervals(now).size)
        assertEquals(parentTaskKey, domainFactory.getTaskForce(childTaskKey).parentTask?.taskKey)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().any { it.taskKey == childTaskKey })

        now += 1.hours

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            EditDelegate.CreateParameters("child task"),
            parentTaskKey,
            null,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(parentInstanceKey, it.single().instanceKey)
            assertEquals(childInstanceKey, it.single().allChildren.single().instanceKey)
        }

        assertEquals(parentTaskKey, domainFactory.getTaskForce(childTaskKey).parentTask?.taskKey)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().any { it.taskKey == childTaskKey })
    }

    @Ignore("todo join test")
    @Test
    fun testSetSingleScheduleForMockChildTaskDifferentTime() {
        val date = Date(2022, 2, 2)

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

        val childTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task"),
            getSingleScheduleData(date, 6, 0),
            null,
        )
            .blockingGet()
            .taskKey

        val (parentInstanceKey, childInstanceKey) = getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)

            it.single { it.taskKey == parentTaskKey }.instanceKey to it.single { it.taskKey == childTaskKey }.instanceKey
        }

        now += 1.hours

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(parentInstanceKey, it.single().instanceKey)
            assertEquals(childInstanceKey, it.single().allChildren.single().instanceKey)
        }

        assertEquals(1, domainFactory.getTaskForce(childTaskKey).intervalInfo.getCurrentScheduleIntervals(now).size)
        assertEquals(parentTaskKey, domainFactory.getTaskForce(childTaskKey).parentTask?.taskKey)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().any { it.taskKey == childTaskKey })

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            EditDelegate.CreateParameters("child task"),
            getSingleScheduleData(date, 7, 0),
            null,
        ).blockingSubscribe()

        assertEquals(setOf(parentInstanceKey, childInstanceKey), getTodayInstanceDatas(now).map { it.instanceKey }.toSet())
        assertNull(domainFactory.getTaskForce(childTaskKey).parentTask)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().none { it.taskKey == childTaskKey })
    }

    @Ignore("todo join test")
    @Test
    fun testCreateMockChildTaskViaEditActivity() {
        val date = Date(2022, 2, 2)

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

        val childTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task"),
            getSingleScheduleData(date, 6, 0),
            null,
        )
            .blockingGet()
            .taskKey

        val (parentInstanceKey, childInstanceKey) = getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)

            it.single { it.taskKey == parentTaskKey }.instanceKey to it.single { it.taskKey == childTaskKey }.instanceKey
        }

        now += 1.hours

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            EditDelegate.CreateParameters("child task"),
            parentTaskKey,
            childInstanceKey,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(parentInstanceKey, it.single().instanceKey)
            assertEquals(childInstanceKey, it.single().allChildren.single().instanceKey)
        }

        assertEquals(1, domainFactory.getTaskForce(childTaskKey).intervalInfo.getCurrentScheduleIntervals(now).size)
        assertEquals(parentTaskKey, domainFactory.getTaskForce(childTaskKey).parentTask?.taskKey)
        assertTrue(domainFactory.getTaskForce(parentTaskKey).getChildTasks().any { it.taskKey == childTaskKey })
    }

    @Test
    fun convertRepeatingScheduleChildWithSubchildToSingle() {
        val date = Date(2022, 3, 10)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            listOf(ScheduleData.Weekly(DayOfWeek.set, TimePair(HourMinute(5, 0)), null, null, 1)),
            null,
        )
            .blockingGet()
            .taskKey

        assertEquals(1, getTodayInstanceDatas(now).size)

        now += 1.hours

        val childTaskCreateParameters = EditDelegate.CreateParameters("child task")

        val childTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            childTaskCreateParameters,
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(1, it.single().allChildren.size)
        }

        now += 1.hours

        val subchildTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(childTaskKey),
            EditDelegate.CreateParameters("subchild task"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

            assertEquals(1, it.single().allChildren.size)

            assertEquals(1, it.single().allChildren.single().allChildren.size)
        }

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            childTaskCreateParameters,
            getSingleScheduleData(date, 6, 0),
            null,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)
        }

        // This checks that an infinite loop doesn't occur in Task.isVisible/getParentInstances
        domainFactory.getTaskForce(subchildTaskKey).isVisible(now)
    }

    @Test
    fun testChildFoundThroughSearch() {
        val today = Date(2022, 4, 7)
        var now = ExactTimeStamp.Local(today, HourMinute(1, 0))

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            listOf(ScheduleData.Weekly(DayOfWeek.set, TimePair(10, 0), null, null, 1)),
            null,
        ).blockingGet()

        val parentInstanceKey = getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)

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

        domainFactory.getSearchInstancesData(
            SearchCriteria(SearchCriteria.Search.Query("child task")),
            0,
        )
            .getDomainResult()
            .data!!
            .groupListDataWrapper
            .allInstanceDatas
            .let {
                assertEquals(1, it.size)

                val parent = it.single()
                val child = parent.allChildren.single()

                assertEquals(childTaskKey, child.instanceKey.taskKey)
            }
    }
}