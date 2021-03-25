package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.hours
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DomainFactoryTest {

    // remember to add overloads to pass in "now" for testing complex scenarios

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    @Test
    fun testCreatingTask() {
        domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "task",
                listOf(ScheduleData.Single(Date(2020, 12, 20), TimePair(HourMinute(20, 0)))),
                null,
                null,
                null,
        ).blockingGet()

        assertEquals(
                "task",
                domainFactory.getMainData()
                        .taskData
                        .entryDatas
                        .single()
                        .children
                        .single()
                        .name
        )
    }

    private fun getTodayInstanceDatas(now: ExactTimeStamp.Local) =
            domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                    .groupListDataWrapper
                    .instanceDatas

    @Test
    fun testCircularDependencyInChildIntervals() {
        val date = Date(2020, 12, 21)
        var now = ExactTimeStamp.Local(date, HourMinute(0, 0))

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(10, 0))))

        val taskName1 = "task1"
        val taskKey1 = domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                taskName1,
                scheduleDatas,
                null,
                null,
                null,
                now = now,
        )
                .blockingGet()
                .taskKey

        now += 1.hours

        val taskName2 = "task2"
        val taskKey2 = domainFactory.createChildTask(
                DomainListenerManager.NotificationType.All,
                taskKey1,
                taskName2,
                null,
                null,
                now = now,
        )
                .blockingGet()
                .taskKey

        assertEquals(taskKey1, getTodayInstanceDatas(now).single().taskKey)
        assertEquals(taskKey2, getTodayInstanceDatas(now).single().children.values.single().taskKey)

        now += 1.hours

        domainFactory.updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                taskKey2,
                taskName2,
                scheduleDatas,
                null,
                null,
                null,
                now,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        now += 1.hours

        domainFactory.updateChildTask(
                DomainListenerManager.NotificationType.All,
                taskKey1,
                taskName1,
                taskKey2,
                null,
                null,
                null,
                true,
                now,
        ).blockingGet()

        domainFactory.getTaskForce(taskKey1).invalidateIntervals()
        domainFactory.getTaskForce(taskKey2).invalidateIntervals()

        assertEquals(taskKey2, getTodayInstanceDatas(now).single().taskKey)
        assertEquals(taskKey1, getTodayInstanceDatas(now).single().children.values.single().taskKey)
    }

    @Test
    fun testGettingParentBasedOnTaskHierarchy() {
        val date = Date(2020, 12, 23)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTask1Key = domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "parentTask1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        val doneChildTaskKey = domainFactory.createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTask1Key,
                "childTask1",
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        val notDoneChildTaskKey = domainFactory.createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTask1Key,
                "childTask2",
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().children.size)

        val doneInstanceKey = getTodayInstanceDatas(now).single()
                .children
                .values
                .single { it.taskKey == doneChildTaskKey }
                .instanceKey

        now += 1.hours

        domainFactory.setInstanceDone(
                DomainListenerManager.NotificationType.All,
                doneInstanceKey,
                true,
                now,
        ).subscribe()

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().children.size)
        assertEquals(1, getTodayInstanceDatas(now).single().children.count { it.value.done != null })

        now += 1.hours

        val parentTask2Key = domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "parentTask2",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
                null,
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        domainFactory.updateChildTask(
                DomainListenerManager.NotificationType.All,
                doneChildTaskKey,
                "childTask1",
                parentTask2Key,
                null,
                null,
                null,
                true,
                now,
        ).blockingGet()

        domainFactory.updateChildTask(
                DomainListenerManager.NotificationType.All,
                notDoneChildTaskKey,
                "childTask2",
                parentTask2Key,
                null,
                null,
                null,
                true,
                now,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        assertEquals(1, getTodayInstanceDatas(now).single { it.taskKey == parentTask1Key }.children.size)
        assertEquals(2, getTodayInstanceDatas(now).single { it.taskKey == parentTask2Key }.children.size)
    }

    @Test
    fun testSettingParentInstanceDoneLocksList() {
        val date = Date(2020, 12, 27)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskKey = domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "parentTask",
                listOf(
                        ScheduleData.Single(date, TimePair(HourMinute(3, 0))),
                        ScheduleData.Single(date, TimePair(HourMinute(4, 0)))
                ),
                null,
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        val firstInstanceDatas = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas

        assertEquals(2, firstInstanceDatas.size)

        val instanceKey1 = firstInstanceDatas[0].instanceKey

        now += 1.hours

        domainFactory.setInstanceDone(
                DomainListenerManager.NotificationType.All,
                instanceKey1,
                true,
                now,
        ).subscribe()

        now += 1.hours

        domainFactory.createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTaskKey,
                "childTask",
                null,
                null,
                null,
                now,
        ).blockingGet()

        val secondInstanceDatas = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas

        assertEquals(2, secondInstanceDatas.size)
        assertEquals(0, secondInstanceDatas[0].children.size)
        assertEquals(1, secondInstanceDatas[1].children.size)
    }

    @Test
    fun testClearingParentWorks() {
        val date = Date(2020, 12, 28)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val task1Key = domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "task1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
                null,
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        assertEquals(
                1,
                domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                        .groupListDataWrapper
                        .instanceDatas
                        .size
        )

        now += 1.hours

        val task2Key = domainFactory.createChildTask(
                DomainListenerManager.NotificationType.All,
                task1Key,
                "task2",
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        val instanceKey = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .let {
                    assertEquals(1, it.size)
                    assertEquals(1, it[0].children.size)

                    it[0].children
                            .values
                            .single()
                            .instanceKey
                }

        val instance = domainFactory.getInstance(instanceKey)
        assertNotNull(instance.parentInstance)

        now += 1.hours

        domainFactory.updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                task2Key,
                "task2",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
                null,
                null,
                null,
                now,
        ).blockingGet()

        domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .let {
                    assertEquals(2, it.size)
                    assertEquals(0, it[0].children.size)
                    assertEquals(0, it[1].children.size)
                }

        assertNull(instance.parentInstance)
    }

    @Test
    fun testInvalidParentAfterJoiningTasks() {
        val date = Date(2021, 1, 10)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "childTask1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
                now,
        ).blockingGet()

        domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "childTask1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
                now,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        val childInstanceKeys = getTodayInstanceDatas(now).map { it.instanceKey }

        now += 2.hours // 3AM

        domainFactory.createScheduleJoinRootTask(
                DomainListenerManager.NotificationType.All,
                "parentTask",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(4, 0)))),
                childInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) },
                null,
                null,
                null,
                true,
                now,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().children.size)
    }

    @Test
    fun testCopyingCustomTime() {
        val date = Date(2021, 3, 25)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val privateTaskKey = domainFactory.createScheduleRootTask(
                DomainListenerManager.NotificationType.All,
                "task",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
                now,
        )
                .blockingGet()
                .taskKey

        val privateCustomTimeKey = domainFactory.createCustomTime(
                DomainListenerManager.NotificationType.All,
                "customTime",
                DayOfWeek.values().associateWith { HourMinute(3, 0) },
        ).blockingGet()

        val sharedProjectKey = domainFactory.createProject(
                DomainListenerManager.NotificationType.All,
                "project",
                setOf(),
        ).blockingGet()

        now += 1.hours // now 2

        domainFactory.updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                privateTaskKey,
                "task",
                listOf(ScheduleData.Single(date, TimePair(privateCustomTimeKey))),
                null,
                EditDelegate.SharedProjectParameters(sharedProjectKey, setOf()),
                null,
                now,
        ).blockingGet()

        domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
    }
}