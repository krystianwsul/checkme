package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
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

        assertEquals(projectKey, task.project.projectKey)
    }

    @Test
    fun testEditInstanceParentSingle() {
        val date = Date(2021, 6, 29)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleData = listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0))))

        val parentTask = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "parent task",
            scheduleData,
            null,
            null,
            null,
        ).blockingGet()

        val childTask = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "child task",
            scheduleData,
            null,
            null,
            null,
        ).blockingGet()

        val instanceDatasBefore = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(2, instanceDatasBefore.size)

        now += 1.hours

        val childInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == childTask.taskKey }
        val parentInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == parentTask.taskKey }

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingGet()

        val instanceDatasAfter = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(1, instanceDatasAfter.size)

        val singleInstanceData = instanceDatasAfter.single()
        assertEquals(parentInstanceKey, singleInstanceData.instanceKey)

        assertEquals(childInstanceKey, singleInstanceData.children.values.single().instanceKey)
    }

    @Test
    fun testEditInstanceParentWeekly() {
        val date = Date(2021, 6, 29)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleData = listOf(
            ScheduleData.Weekly(
                setOf(DayOfWeek.TUESDAY),
                TimePair(HourMinute(2, 0)),
                null,
                null,
                1,
            )
        )

        val parentTask = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "parent task",
            scheduleData,
            null,
            null,
            null,
        ).blockingGet()

        val childTask = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "child task",
            scheduleData,
            null,
            null,
            null,
        ).blockingGet()

        val instanceDatasBefore = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(2, instanceDatasBefore.size)

        now += 1.hours

        val childInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == childTask.taskKey }
        val parentInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == parentTask.taskKey }

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingGet()

        val instanceDatasAfter = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(1, instanceDatasAfter.size)

        val singleInstanceData = instanceDatasAfter.single()
        assertEquals(parentInstanceKey, singleInstanceData.instanceKey)

        assertEquals(childInstanceKey, singleInstanceData.children.values.single().instanceKey)
    }

    @Test
    fun testChangingProjectForChildTask() {
        val today = Date(2021, 6, 30)

        var now = ExactTimeStamp.Local(today, HourMinute(1, 0))

        val sharedProjectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project",
            emptySet(),
        ).blockingGet()

        now += 1.hours

        val scheduleDatas = listOf(ScheduleData.Single(today, TimePair(HourMinute(4, 0))))

        val parentTaskName = "parentTask"

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            parentTaskName,
            scheduleDatas,
            null,
            null,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val privateProjectKey = domainFactory.defaultProjectKey

        val parentTask = domainFactory.getTaskForce(parentTaskKey) as RootTask
        assertEquals(privateProjectKey, parentTask.project.projectKey)

        val childTaskKey = domainUpdater(now).createChildTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            "child task",
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val childTask = domainFactory.getTaskForce(childTaskKey) as RootTask
        assertEquals(privateProjectKey, childTask.project.projectKey)

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            parentTaskName,
            scheduleDatas,
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey, emptySet()),
            null,
        ).blockingGet()

        assertEquals(sharedProjectKey, parentTask.project.projectKey)
        assertEquals(sharedProjectKey, childTask.project.projectKey)
    }

    @Test
    fun testInstancesInvalidate() {
        val date = Date(2021, 7, 10)
        val hourMinute1 = HourMinute(1, 0)
        val hourMinute5 = HourMinute(5, 0)

        var now = ExactTimeStamp.Local(date, hourMinute1)

        val parentTaskNameBefore = "parent task"

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            parentTaskNameBefore,
            listOf(ScheduleData.Single(date, TimePair(hourMinute5))),
            null,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        fun getGroupListData() = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(parentTaskNameBefore, getGroupListData().single().name)

        now += 1.hours

        domainUpdater(now).createChildTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            "child task",
            null,
            null,
        ).blockingGet()

        assertEquals(parentTaskNameBefore, getGroupListData().single().name)

        fun getJson(taskKey: TaskKey.Root) = (domainFactory.getTaskForce(taskKey) as RootTask).taskRecord.createObject

        val parentTaskNameAfter = "parent task renamed"

        val parentTaskJson = getJson(parentTaskKey).copy(name = parentTaskNameAfter)

        domainFactoryRule.acceptRootTaskJson(parentTaskKey, parentTaskJson)
        assertEquals(parentTaskNameAfter, getGroupListData().single().name)
    }

    @Test
    fun testJoinTwoSingleScheduleDifferentProjects() {
        val date = Date(2021, 7, 13)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val privateProjectKey = domainFactory.projectsFactory
            .privateProject
            .projectKey

        val sharedProjectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project",
            setOf(),
        ).blockingGet()

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0))))

        val privateTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "private task",
            scheduleDatas,
            null,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val sharedTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "shared task",
            scheduleDatas,
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey, setOf()),
            null,
        )
            .blockingGet()
            .taskKey

        val instanceDatas = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(2, instanceDatas.size)
        assertEquals(null, instanceDatas[0].projectKey)
        assertEquals(sharedProjectKey, instanceDatas[1].projectKey)

        val privateInstanceKey = instanceDatas[0].instanceKey
        val sharedInstanceKey = instanceDatas[1].instanceKey

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "join task",
            scheduleDatas,
            listOf(
                EditParameters.Join.Joinable.Instance(privateInstanceKey),
                EditParameters.Join.Joinable.Instance(sharedInstanceKey),
            ),
            null,
            null,
            null,
            true,
        ).blockingGet()

        fun getProjectKey(taskKey: TaskKey) = domainFactory.getTaskForce(taskKey)
            .project
            .projectKey

        assertEquals(privateProjectKey, getProjectKey(joinTaskKey))
        assertEquals(privateProjectKey, getProjectKey(privateTaskKey))
        assertEquals(privateProjectKey, getProjectKey(sharedTaskKey))

        val instanceData = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas
            .single()

        assertEquals(null, instanceData.projectKey)
        assertEquals(2, instanceData.children.size)
        assertTrue(instanceData.children.values.all { it.projectKey == null })
    }

    @Test
    fun testJoinSingleAndWeeklyScheduleDifferentProjectsJustTheseReminders() {
        val date = Date(2021, 7, 13)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val privateProjectKey = domainFactory.projectsFactory
            .privateProject
            .projectKey

        val sharedProjectKey1 = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project 1",
            setOf(),
        ).blockingGet()

        val sharedProjectKey2 = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project 2",
            setOf(),
        ).blockingGet()

        val scheduleTimePair = TimePair(HourMinute(5, 0))

        val singleScheduleDatas = listOf(ScheduleData.Single(date, scheduleTimePair))

        val taskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "task 1 single",
            singleScheduleDatas,
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey1, setOf()),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val taskKey2 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "task 2 weekly",
            listOf(ScheduleData.Weekly(setOf(DayOfWeek.TUESDAY), scheduleTimePair, null, null, 1)),
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey2, setOf()),
            null,
        )
            .blockingGet()
            .taskKey

        val instanceDatas = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(2, instanceDatas.size)
        assertEquals(sharedProjectKey1, instanceDatas[0].projectKey)
        assertEquals(sharedProjectKey2, instanceDatas[1].projectKey)

        val instanceKey1 = instanceDatas[0].instanceKey
        val instanceKey2 = instanceDatas[1].instanceKey

        assertEquals(
            sharedProjectKey2,
            domainFactory.getGroupListData(now, 7, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .single()
                .projectKey,
        )

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "join task",
            singleScheduleDatas,
            listOf(EditParameters.Join.Joinable.Instance(instanceKey1), EditParameters.Join.Joinable.Instance(instanceKey2)),
            null,
            null,
            null,
            false,
        ).blockingGet()

        fun getProjectKey(taskKey: TaskKey) = domainFactory.getTaskForce(taskKey)
            .project
            .projectKey

        assertEquals(privateProjectKey, getProjectKey(joinTaskKey))
        assertEquals(privateProjectKey, getProjectKey(taskKey1))
        assertEquals(sharedProjectKey2, getProjectKey(taskKey2))

        val instanceData = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas
            .single()

        assertEquals(null, instanceData.projectKey)
        assertEquals(2, instanceData.children.size)
        assertTrue(instanceData.children.values.all { it.projectKey == null })

        assertEquals(
            sharedProjectKey2,
            domainFactory.getGroupListData(now, 7, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .single()
                .projectKey
        )
    }

    @Test
    fun testJoinSingleAndWeeklyScheduleDifferentProjectsAllReminders() {
        val date = Date(2021, 7, 13)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val privateProjectKey = domainFactory.projectsFactory
            .privateProject
            .projectKey

        val sharedProjectKey1 = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project 1",
            setOf(),
        ).blockingGet()

        val sharedProjectKey2 = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project 2",
            setOf(),
        ).blockingGet()

        val scheduleTimePair = TimePair(HourMinute(5, 0))

        val singleScheduleDatas = listOf(ScheduleData.Single(date, scheduleTimePair))

        val taskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "task 1 single",
            singleScheduleDatas,
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey1, setOf()),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val taskKey2 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "task 2 weekly",
            listOf(ScheduleData.Weekly(setOf(DayOfWeek.TUESDAY), scheduleTimePair, null, null, 1)),
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey2, setOf()),
            null,
        )
            .blockingGet()
            .taskKey

        val instanceDatas = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas

        assertEquals(2, instanceDatas.size)
        assertEquals(sharedProjectKey1, instanceDatas[0].projectKey)
        assertEquals(sharedProjectKey2, instanceDatas[1].projectKey)

        val instanceKey1 = instanceDatas[0].instanceKey
        val instanceKey2 = instanceDatas[1].instanceKey

        assertEquals(
            sharedProjectKey2,
            domainFactory.getGroupListData(now, 7, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .single()
                .projectKey,
        )

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "join task",
            singleScheduleDatas,
            listOf(EditParameters.Join.Joinable.Instance(instanceKey1), EditParameters.Join.Joinable.Instance(instanceKey2)),
            null,
            null,
            null,
            true,
        ).blockingGet()

        fun getProjectKey(taskKey: TaskKey) = domainFactory.getTaskForce(taskKey)
            .project
            .projectKey

        assertEquals(privateProjectKey, getProjectKey(joinTaskKey))
        assertEquals(privateProjectKey, getProjectKey(taskKey1))
        assertEquals(privateProjectKey, getProjectKey(taskKey2))

        val instanceData = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas
            .single()

        assertEquals(null, instanceData.projectKey)
        assertEquals(2, instanceData.children.size)
        assertTrue(instanceData.children.values.all { it.projectKey == null })

        assertTrue(
            domainFactory.getGroupListData(now, 7, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .isEmpty()
        )
    }

    @Test
    fun testTaskHierarchyCycle() {
        val date = Date(2021, 7, 14)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0))))

        val taskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "task1",
            scheduleDatas,
            null,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val taskKey2 = domainUpdater(now).createChildTask(
            DomainListenerManager.NotificationType.All,
            taskKey1,
            "task2",
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val taskKey3 = domainUpdater(now).createChildTask(
            DomainListenerManager.NotificationType.All,
            taskKey2,
            "task3",
            null,
            null,
        )
            .blockingGet()
            .taskKey

        assertEquals(
            taskKey1,
            domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .single()
                .instanceKey
                .taskKey,
        )

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey3,
            "task3",
            scheduleDatas,
            null,
            null,
            null,
        )

        assertEquals(
            2,
            domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas
                .size,
        )

        now += 1.hours

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            taskKey1,
            "task1",
            taskKey3,
            null,
            null,
            null,
            true,
        )

        val instanceKey3 = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .instanceDatas
            .single()
            .instanceKey

        assertEquals(taskKey3, instanceKey3.taskKey)

        assertEquals(
            1,
            domainFactory.getShowInstanceData(instanceKey3)
                .groupListDataWrapper
                .instanceDatas
                .size,
        )
    }
}