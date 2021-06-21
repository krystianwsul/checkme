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

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
            TestDomainUpdater(domainFactory, now)

    @Test
    fun testCreatingTask() {
        domainUpdater().createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "task",
                listOf(ScheduleData.Single(Date(2020, 12, 20), TimePair(HourMinute(20, 0)))),
                null,
                null,
                null,
        ).blockingGet()

        assertEquals(
                "task",
                domainFactory.getMainTaskData()
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
        val taskKey1 = domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                taskName1,
                scheduleDatas,
                null,
                null,
                null,
        )
                .blockingGet()
                .taskKey

        now += 1.hours

        val taskName2 = "task2"
        val taskKey2 = domainUpdater(now).createChildTask(
                DomainListenerManager.NotificationType.All,
                taskKey1,
                taskName2,
                null,
                null,
        )
                .blockingGet()
                .taskKey

        assertEquals(taskKey1, getTodayInstanceDatas(now).single().taskKey)
        assertEquals(taskKey2, getTodayInstanceDatas(now).single().children.values.single().taskKey)

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                taskKey2,
                taskName2,
                scheduleDatas,
                null,
                null,
                null,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        now += 1.hours

        domainUpdater(now).updateChildTask(
                DomainListenerManager.NotificationType.All,
                taskKey1,
                taskName1,
                taskKey2,
                null,
                null,
                null,
                true,
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

        val parentTask1Key = domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "parentTask1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
        )
                .blockingGet()
                .taskKey

        val doneChildTaskKey = domainUpdater(now).createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTask1Key,
                "childTask1",
                null,
                null,
                null,
        )
                .blockingGet()
                .taskKey

        val notDoneChildTaskKey = domainUpdater(now).createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTask1Key,
                "childTask2",
                null,
                null,
                null,
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

        domainUpdater(now).setInstanceDone(
                DomainListenerManager.NotificationType.All,
                doneInstanceKey,
                true,
        ).subscribe()

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().children.size)
        assertEquals(1, getTodayInstanceDatas(now).single().children.count { it.value.done != null })

        now += 1.hours

        val parentTask2Key = domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "parentTask2",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
                null,
                null,
                null,
                null,
        )
                .blockingGet()
                .taskKey

        domainUpdater(now).updateChildTask(
                DomainListenerManager.NotificationType.All,
                doneChildTaskKey,
                "childTask1",
                parentTask2Key,
                null,
                null,
                null,
                true,
        ).blockingGet()

        domainUpdater(now).updateChildTask(
                DomainListenerManager.NotificationType.All,
                notDoneChildTaskKey,
                "childTask2",
                parentTask2Key,
                null,
                null,
                null,
                true,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        assertEquals(1, getTodayInstanceDatas(now).single { it.taskKey == parentTask1Key }.children.size)
        assertEquals(2, getTodayInstanceDatas(now).single { it.taskKey == parentTask2Key }.children.size)
    }

    @Test
    fun testSettingParentInstanceDoneLocksList() {
        val date = Date(2020, 12, 27)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
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
        )
                .blockingGet()
                .taskKey

        val firstInstanceDatas = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas

        assertEquals(2, firstInstanceDatas.size)

        val instanceKey1 = firstInstanceDatas[0].instanceKey

        now += 1.hours

        domainUpdater(now).setInstanceDone(
                DomainListenerManager.NotificationType.All,
                instanceKey1,
                true,
        ).subscribe()

        now += 1.hours

        domainUpdater(now).createChildTask(
                DomainListenerManager.NotificationType.All,
                parentTaskKey,
                "childTask",
                null,
                null,
                null,
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

        val task1Key = domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "task1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
                null,
                null,
                null,
                null,
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

        val task2Key = domainUpdater(now).createChildTask(
                DomainListenerManager.NotificationType.All,
                task1Key,
                "task2",
                null,
                null,
                null,
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

        domainUpdater(now).updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                task2Key,
                "task2",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
                null,
                null,
                null,
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

        domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "childTask1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
        ).blockingGet()

        domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "childTask1",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
                null,
                null,
                null,
                null,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        val childInstanceKeys = getTodayInstanceDatas(now).map { it.instanceKey }

        now += 2.hours // 3AM

        domainUpdater(now).createScheduleJoinTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "parentTask",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(4, 0)))),
                childInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) },
                null,
                null,
                null,
                true,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().children.size)
    }

    @Test
    fun testCopyingCustomTime() {
        val date = Date(2021, 3, 25)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val customTimeKey = domainUpdater().createCustomTime(
                DomainListenerManager.NotificationType.All,
                "customTime",
                DayOfWeek.values().associateWith { HourMinute(2, 0) },
        ).blockingGet()

        val privateTaskKey = domainUpdater(now).createScheduleTopLevelTask(
                DomainListenerManager.NotificationType.All,
                "task",
                listOf(ScheduleData.Single(date, TimePair(customTimeKey))),
                null,
                null,
                null,
                null,
        )
                .blockingGet()
                .taskKey

        val sharedProjectKey = domainUpdater().createProject(
                DomainListenerManager.NotificationType.All,
                "project",
                setOf(),
        ).blockingGet()

        now += 1.hours // now 2

        domainUpdater(now).updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                privateTaskKey,
                "task",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
                null,
                null,
                null,
        ).blockingGet()

        val instanceKey = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .single()
                .instanceKey

        now += 1.hours // now 3

        domainUpdater(now).updateScheduleTask(
                DomainListenerManager.NotificationType.All,
                privateTaskKey,
                "task",
                listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
                null,
                EditDelegate.SharedProjectParameters(sharedProjectKey, setOf()),
                null,
        ).blockingGet()

        domainFactory.getShowInstanceData(instanceKey)

        assertEquals(
            1,
            domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .size,
        )
    }

    @Test
    fun testDeletingTask() {
        val today = Date(2021, 5, 29)
        val hourMinute1 = HourMinute(1, 0)
        val hourMinute2 = HourMinute(2, 0)

        var now = ExactTimeStamp.Local(today, hourMinute1)

        val taskName = "taskName"

        val createResult = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            taskName,
            listOf(ScheduleData.Single(Date(2021, 12, 20), TimePair(HourMinute(20, 0)))),
            null,
            null,
            null,
        ).blockingGet()

        assertEquals(
            taskName,
            domainFactory.getMainTaskData()
                .taskData
                .entryDatas
                .single()
                .children
                .single()
                .name
        )

        val taskKey = createResult.taskKey

        val task = domainFactory.getTaskForce(taskKey)

        val projectKey = task.project.projectKey

        now = ExactTimeStamp.Local(today, hourMinute2)

        domainUpdater(now).setTaskEndTimeStamps(
            DomainListenerManager.NotificationType.All,
            setOf(taskKey),
            false,
        ).blockingGet()

        assertEquals(projectKey, task.project.projectKey)
    }

    @Test
    fun testChangingProjectForDailyTask() {
        val today = Date(2021, 6, 21)
        val hour1 = HourMinute(11, 0)
        val hour2 = HourMinute(14, 0)

        var now = ExactTimeStamp.Local(today, hour1)

        val projectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project",
            emptySet(),
        ).blockingGet()

        now += 1.hours

        val taskName = "taskName"
        val scheduleDatas = listOf(ScheduleData.Weekly(DayOfWeek.set, TimePair(hour2), null, null, 1))

        val taskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            taskName,
            listOf(ScheduleData.Weekly(DayOfWeek.set, TimePair(hour2), null, null, 1)),
            null,
            null,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey,
            taskName,
            scheduleDatas,
            null,
            EditDelegate.SharedProjectParameters(projectKey, emptySet()),
            null,
        ).blockingGet()

        val task = domainFactory.rootTasksFactory.getTask(taskKey)

        assertTrue(
            task.intervalInfo
                .getCurrentScheduleIntervals(now)
                .all { it.schedule.projectId == projectKey.key }
        )
    }
}