package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.domainmodel.updates.CreateChildTaskDomainUpdate
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.InstanceKey
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
        val date = Date(2020, 12, 20)
        val now = ExactTimeStamp.Local(date, HourMinute(0, 0))

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(20, 0)))),
            null,
        ).blockingGet()

        assertEquals(
            "task",
            domainFactory.getMainTaskData(false, SearchCriteria.empty, now)
                .taskData
                .entryDatas
                .single()
                .name,
        )
    }

    private fun getTodayGroupListData(now: ExactTimeStamp.Local, position: Int = 0) =
        domainFactory.getGroupListData(now, position, Preferences.TimeRange.DAY, false)

    private fun getTodayInstanceDatas(now: ExactTimeStamp.Local, position: Int = 0) =
        getTodayGroupListData(now, position).groupListDataWrapper.allInstanceDatas

    @Test
    fun testCircularDependencyInChildIntervals() {
        val date = Date(2020, 12, 21)
        var now = ExactTimeStamp.Local(date, HourMinute(0, 0))

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(10, 0))))

        val taskName1 = "task1"
        val taskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters(taskName1),
            scheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val taskName2 = "task2"
        val taskKey2 = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(taskKey1),
            EditDelegate.CreateParameters(taskName2),
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        assertEquals(taskKey1, getTodayInstanceDatas(now).single().taskKey)
        assertEquals(taskKey2, getTodayInstanceDatas(now).single().allChildren.single().taskKey)

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey2,
            EditDelegate.CreateParameters(taskName2),
            scheduleDatas,
            null,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        now += 1.hours

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            taskKey1,
            EditDelegate.CreateParameters(taskName1),
            taskKey2,
            null,
            true,
        ).blockingGet()

        domainFactory.getTaskForce(taskKey1).invalidateIntervals()
        domainFactory.getTaskForce(taskKey2).invalidateIntervals()

        assertEquals(taskKey2, getTodayInstanceDatas(now).single().taskKey)
        assertEquals(taskKey1, getTodayInstanceDatas(now).single().allChildren.single().taskKey)
    }

    @Test
    fun testGettingParentBasedOnTaskHierarchy() {
        val date = Date(2020, 12, 23)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTask1Key = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parentTask1"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val doneChildTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTask1Key),
            EditDelegate.CreateParameters("childTask1"),
            null,
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        val notDoneChildTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTask1Key),
            EditDelegate.CreateParameters("childTask2"),
            null,
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().allChildren.size)

        val doneInstanceKey = getTodayInstanceDatas(now).single()
            .allChildren
            .single { it.taskKey == doneChildTaskKey }
            .instanceKey

        now += 1.hours

        domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            doneInstanceKey,
            true,
        ).subscribe()

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().allChildren.size)
        assertEquals(1, getTodayInstanceDatas(now).single().allChildren.count { it.done != null })

        now += 1.hours

        val parentTask2Key = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parentTask2"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
            null,
            null,
        )
            .blockingGet()
            .taskKey

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            doneChildTaskKey,
            EditDelegate.CreateParameters("childTask1"),
            parentTask2Key,
            null,
            true,
        ).blockingGet()

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            notDoneChildTaskKey,
            EditDelegate.CreateParameters("childTask2"),
            parentTask2Key,
            null,
            true,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        assertEquals(1, getTodayInstanceDatas(now).single { it.taskKey == parentTask1Key }.allChildren.size)
        assertEquals(2, getTodayInstanceDatas(now).single { it.taskKey == parentTask2Key }.allChildren.size)
    }

    @Test
    fun testSettingParentInstanceDoneLocksList() {
        val date = Date(2020, 12, 27)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parentTask"),
            listOf(
                ScheduleData.Single(date, TimePair(HourMinute(3, 0))),
                ScheduleData.Single(date, TimePair(HourMinute(4, 0)))
            ),
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val firstInstanceDatas = getTodayInstanceDatas(now)

        assertEquals(2, firstInstanceDatas.size)

        val instanceKey1 = firstInstanceDatas[0].instanceKey

        now += 1.hours

        domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            instanceKey1,
            true,
        ).subscribe()

        now += 1.hours

        CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("childTask"),
            null,
        ).perform(domainUpdater(now)).blockingGet()

        val secondGroupListWrapper = getTodayGroupListData(now).groupListDataWrapper

        assertEquals(0, secondGroupListWrapper.doneSingleBridges.single().instanceData.allChildren.size)
        assertEquals(
            1,
            secondGroupListWrapper.mixedInstanceDataCollection.instanceDatas.single().allChildren.size
        )
    }

    @Test
    fun testClearingParentWorks() {
        val date = Date(2020, 12, 28)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val task1Key = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task1"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
            null,
            null,
        )
            .blockingGet()
            .taskKey

        assertEquals(1, getTodayInstanceDatas(now).size)

        now += 1.hours

        val task2Key = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(task1Key),
            EditDelegate.CreateParameters("task2"),
            null,
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        val instanceKey = getTodayInstanceDatas(now).let {
            assertEquals(1, it.size)
            assertEquals(1, it[0].allChildren.size)

            it[0].allChildren
                .single()
                .instanceKey
        }

        val instance = domainFactory.getInstance(instanceKey)
        assertNotNull(instance.parentInstance)

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            task2Key,
            EditDelegate.CreateParameters("task2"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
            null,
        ).blockingGet()

        getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)
            assertEquals(0, it[0].allChildren.size)
            assertEquals(0, it[1].allChildren.size)
        }

        assertNull(instance.parentInstance)
    }

    @Test
    fun testInvalidParentAfterJoiningTasks() {
        val date = Date(2021, 1, 10)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("childTask1"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
            null,
            null,
        ).blockingGet()

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("childTask1"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(2, 0)))),
            null,
            null,
        ).blockingGet()

        assertEquals(2, getTodayInstanceDatas(now).size)

        val childInstanceKeys = getTodayInstanceDatas(now).map { it.instanceKey }

        now += 2.hours // 3AM

        domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parentTask"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(4, 0)))),
            childInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) },
            null,
            true,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now).single().allChildren.size)
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
            EditDelegate.CreateParameters("task"),
            listOf(ScheduleData.Single(date, TimePair(customTimeKey))),
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
            EditDelegate.CreateParameters("task"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
            null,
        ).blockingGet()

        val instanceKey = getTodayInstanceDatas(now).single().instanceKey

        now += 1.hours // now 3

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            privateTaskKey,
            EditDelegate.CreateParameters("task"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(3, 0)))),
            EditDelegate.SharedProjectParameters(sharedProjectKey, setOf()),
        ).blockingGet()

        domainFactory.getShowInstanceData(instanceKey, SearchCriteria.empty)

        assertEquals(1, getTodayInstanceDatas(now).size)
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
            EditDelegate.CreateParameters(taskName),
            listOf(ScheduleData.Single(Date(2021, 12, 20), TimePair(HourMinute(20, 0)))),
            null,
        ).blockingGet()

        assertEquals(
            taskName,
            domainFactory.getMainTaskData(false, SearchCriteria.empty, now)
                .taskData
                .entryDatas
                .single()
                .name,
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
            EditDelegate.CreateParameters(taskName),
            listOf(ScheduleData.Weekly(DayOfWeek.set, TimePair(hour2), null, null, 1)),
            null,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey,
            EditDelegate.CreateParameters(taskName),
            scheduleDatas,
            EditDelegate.SharedProjectParameters(projectKey, emptySet()),
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
            EditDelegate.CreateParameters("parent task"),
            scheduleData,
            null,
        ).blockingGet()

        val childTask = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task"),
            scheduleData,
            null,
        ).blockingGet()

        val instanceDatasBefore = getTodayInstanceDatas(now)

        assertEquals(2, instanceDatasBefore.size)

        now += 1.hours

        val childInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == childTask.taskKey }
        val parentInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == parentTask.taskKey }

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingGet()

        val instanceDatasAfter = getTodayInstanceDatas(now)

        assertEquals(1, instanceDatasAfter.size)

        val singleInstanceData = instanceDatasAfter.single()
        assertEquals(parentInstanceKey, singleInstanceData.instanceKey)

        assertEquals(childInstanceKey, singleInstanceData.allChildren.single().instanceKey)
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
            EditDelegate.CreateParameters("parent task"),
            scheduleData,
            null,
        ).blockingGet()

        val childTask = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task"),
            scheduleData,
            null,
        ).blockingGet()

        val instanceDatasBefore = getTodayInstanceDatas(now)

        assertEquals(2, instanceDatasBefore.size)

        now += 1.hours

        val childInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == childTask.taskKey }
        val parentInstanceKey = instanceDatasBefore.map { it.instanceKey }.single { it.taskKey == parentTask.taskKey }

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(childInstanceKey),
            parentInstanceKey,
        ).blockingGet()

        val instanceDatasAfter = getTodayInstanceDatas(now)

        assertEquals(1, instanceDatasAfter.size)

        val singleInstanceData = instanceDatasAfter.single()
        assertEquals(parentInstanceKey, singleInstanceData.instanceKey)

        assertEquals(childInstanceKey, singleInstanceData.allChildren.single().instanceKey)
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
            EditDelegate.CreateParameters(parentTaskName),
            scheduleDatas,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val privateProjectKey = domainFactory.defaultProjectKey

        val parentTask = domainFactory.getTaskForce(parentTaskKey) as RootTask
        assertEquals(privateProjectKey, parentTask.project.projectKey)

        val childTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("child task"),
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        val childTask = domainFactory.getTaskForce(childTaskKey) as RootTask
        assertEquals(privateProjectKey, childTask.project.projectKey)

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            EditDelegate.CreateParameters(parentTaskName),
            scheduleDatas,
            EditDelegate.SharedProjectParameters(sharedProjectKey, emptySet()),
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
            EditDelegate.CreateParameters(parentTaskNameBefore),
            listOf(ScheduleData.Single(date, TimePair(hourMinute5))),
            null,
        )
            .blockingGet()
            .taskKey

        assertEquals(parentTaskNameBefore, getTodayInstanceDatas(now).single().name)

        now += 1.hours

        CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            EditDelegate.CreateParameters("child task"),
        ).perform(domainUpdater(now)).blockingGet()

        assertEquals(parentTaskNameBefore, getTodayInstanceDatas(now).single().name)

        fun getJson(taskKey: TaskKey.Root) = (domainFactory.getTaskForce(taskKey) as RootTask).taskRecord.createObject

        val parentTaskNameAfter = "parent task renamed"

        val parentTaskJson = getJson(parentTaskKey).copy(name = parentTaskNameAfter)

        domainFactoryRule.acceptRootTaskJson(parentTaskKey, parentTaskJson)
        assertEquals(parentTaskNameAfter, getTodayInstanceDatas(now).single().name)
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
            EditDelegate.CreateParameters("private task"),
            scheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val sharedTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("shared task"),
            scheduleDatas,
            EditDelegate.SharedProjectParameters(sharedProjectKey, setOf()),
        )
            .blockingGet()
            .taskKey

        val instanceDatas = getTodayInstanceDatas(now)

        assertEquals(2, instanceDatas.size)
        assertEquals(null, instanceDatas[0].projectKey)
        assertEquals(sharedProjectKey, instanceDatas[1].projectKey)

        val privateInstanceKey = instanceDatas[0].instanceKey
        val sharedInstanceKey = instanceDatas[1].instanceKey

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("join task"),
            scheduleDatas,
            listOf(
                EditParameters.Join.Joinable.Instance(privateInstanceKey),
                EditParameters.Join.Joinable.Instance(sharedInstanceKey),
            ),
            null,
            true,
        ).blockingGet()

        fun getProjectKey(taskKey: TaskKey) = domainFactory.getTaskForce(taskKey)
            .project
            .projectKey

        assertEquals(privateProjectKey, getProjectKey(joinTaskKey))
        assertEquals(privateProjectKey, getProjectKey(privateTaskKey))
        assertEquals(privateProjectKey, getProjectKey(sharedTaskKey))

        val instanceData = getTodayInstanceDatas(now).single()

        assertEquals(null, instanceData.projectKey)
        assertEquals(2, instanceData.allChildren.size)
        assertTrue(instanceData.allChildren.all { it.projectKey == null })
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

        /*val taskKey1 = */domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task 1 single"),
            singleScheduleDatas,
            EditDelegate.SharedProjectParameters(sharedProjectKey1, setOf()),
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val taskKey2 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task 2 weekly"),
            listOf(ScheduleData.Weekly(setOf(DayOfWeek.TUESDAY), scheduleTimePair, null, null, 1)),
            EditDelegate.SharedProjectParameters(sharedProjectKey2, setOf()),
        )
            .blockingGet()
            .taskKey

        val instanceDatas = getTodayInstanceDatas(now)

        assertEquals(2, instanceDatas.size)
        assertEquals(sharedProjectKey1, instanceDatas[0].projectKey)
        assertEquals(sharedProjectKey2, instanceDatas[1].projectKey)

        val instanceKey1 = instanceDatas[0].instanceKey
        val instanceKey2 = instanceDatas[1].instanceKey

        assertEquals(sharedProjectKey2, getTodayInstanceDatas(now, 7).single().projectKey)

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("join task"),
            singleScheduleDatas,
            listOf(EditParameters.Join.Joinable.Instance(instanceKey1), EditParameters.Join.Joinable.Instance(instanceKey2)),
            null,
            false,
        ).blockingGet()

        fun getProjectKey(taskKey: TaskKey) = domainFactory.getTaskForce(taskKey)
            .project
            .projectKey

        assertEquals(privateProjectKey, getProjectKey(joinTaskKey))
        assertEquals(sharedProjectKey2, getProjectKey(taskKey2))

        val instanceData = getTodayInstanceDatas(now).single()

        assertEquals(null, instanceData.projectKey)
        assertEquals(2, instanceData.allChildren.size)
        assertTrue(instanceData.allChildren.all { it.projectKey == null })

        assertEquals(sharedProjectKey2, getTodayInstanceDatas(now, 7).single().projectKey)
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
            EditDelegate.CreateParameters("task 1 single"),
            singleScheduleDatas,
            EditDelegate.SharedProjectParameters(sharedProjectKey1, setOf()),
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val taskKey2 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task 2 weekly"),
            listOf(ScheduleData.Weekly(setOf(DayOfWeek.TUESDAY), scheduleTimePair, null, null, 1)),
            EditDelegate.SharedProjectParameters(sharedProjectKey2, setOf()),
        )
            .blockingGet()
            .taskKey

        val instanceDatas = getTodayInstanceDatas(now)

        assertEquals(2, instanceDatas.size)
        assertEquals(sharedProjectKey1, instanceDatas[0].projectKey)
        assertEquals(sharedProjectKey2, instanceDatas[1].projectKey)

        val instanceKey1 = instanceDatas[0].instanceKey
        val instanceKey2 = instanceDatas[1].instanceKey

        assertEquals(sharedProjectKey2, getTodayInstanceDatas(now, 7).single().projectKey)

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("join task"),
            singleScheduleDatas,
            listOf(EditParameters.Join.Joinable.Instance(instanceKey1), EditParameters.Join.Joinable.Instance(instanceKey2)),
            null,
            true,
        ).blockingGet()

        fun getProjectKey(taskKey: TaskKey) = domainFactory.getTaskForce(taskKey)
            .project
            .projectKey

        assertEquals(privateProjectKey, getProjectKey(joinTaskKey))
        assertEquals(privateProjectKey, getProjectKey(taskKey1))
        assertEquals(privateProjectKey, getProjectKey(taskKey2))

        val instanceData = getTodayInstanceDatas(now).single()

        assertEquals(null, instanceData.projectKey)
        assertEquals(2, instanceData.allChildren.size)
        assertTrue(instanceData.allChildren.all { it.projectKey == null })

        assertTrue(getTodayInstanceDatas(now, 7).isEmpty())
    }

    @Test
    fun testTaskHierarchyCycle() {
        val date = Date(2021, 7, 14)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0))))

        val taskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("task1"),
            scheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        val taskKey2 = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(taskKey1),
            EditDelegate.CreateParameters("task2"),
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        val taskKey3 = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(taskKey2),
            EditDelegate.CreateParameters("task3"),
        )
            .perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        assertEquals(
            taskKey1,
            getTodayInstanceDatas(now).single()
                .instanceKey
                .taskKey,
        )

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey3,
            EditDelegate.CreateParameters("task3"),
            scheduleDatas,
            null,
        ).blockingSubscribe()

        assertEquals(2, getTodayInstanceDatas(now).size)

        now += 1.hours

        domainUpdater(now).updateChildTask(
            DomainListenerManager.NotificationType.All,
            taskKey1,
            EditDelegate.CreateParameters("task1"),
            taskKey3,
            null,
            true,
        ).blockingSubscribe()

        val instanceKey3 = getTodayInstanceDatas(now).single().instanceKey

        assertEquals(taskKey3, instanceKey3.taskKey)

        val showInstanceData = domainFactory.getShowInstanceData(instanceKey3, SearchCriteria.empty, now)

        val instanceData1 = showInstanceData.groupListDataWrapper
            .allInstanceDatas
            .single()

        assertEquals(taskKey1, instanceData1.instanceKey.taskKey)

        val instanceData2 = instanceData1.allChildren.single()

        assertEquals(taskKey2, instanceData2.instanceKey.taskKey)
        assertTrue(instanceData2.allChildren.isEmpty())

        listOf(taskKey1, taskKey2, taskKey3).forEach {
            domainFactory.rootTasksFactory
                .getRootTask(it)
                .rootTaskDependencyResolver
                .invalidate()
        }

        domainFactory.projectsFactory
            .privateProject
            .rootTasksCache
            .invalidate()

        assertEquals(
            taskKey3,
            getTodayInstanceDatas(now).single()
                .instanceKey
                .taskKey,
        )
    }

    @Test
    fun testInnerJoinAllInstances() {
        val date = Date(2021, 11, 1)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleTimePair = TimePair(HourMinute(5, 0))

        val repeatingScheduleDatas = listOf(
            ScheduleData.Weekly(DayOfWeek.set, scheduleTimePair, null, null, 1)
        )

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("repeating 1"),
            repeatingScheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("repeating 2"),
            repeatingScheduleDatas,
            null
        )
            .blockingGet()
            .taskKey

        assertEquals(2, getTodayInstanceDatas(now).size)
        assertEquals(2, getTodayInstanceDatas(now, 1).size)

        now += 1.hours

        val singleScheduleDatas = listOf(ScheduleData.Single(date, scheduleTimePair))

        val repeatingInstanceKeys = getTodayInstanceDatas(now).map { it.instanceKey }

        val joinables = repeatingInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) }

        val outerJoinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("outer join"),
            singleScheduleDatas,
            joinables,
            null,
            false,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)

        assertEquals(
            2,
            getTodayInstanceDatas(now).single()
                .allChildren
                .size,
        )

        assertEquals(2, getTodayInstanceDatas(now, 1).size)

        now += 1.hours

        /*
        as-is, this SHOULD result in a tree:

        outer join
        -inner join
        --repeating 1
        --repeating 2

        And should remove the instances for the next day.

        This is as-is, despite the fact that I should support joining instances in this flow (support joinables as param),
        which should give yet another different behavior.
         */
        domainUpdater(now).createJoinChildTask(
            DomainListenerManager.NotificationType.All,
            outerJoinTaskKey,
            EditDelegate.CreateParameters("inner join"),
            repeatingInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) },
            true,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)

        assertEquals(
            1,
            getTodayInstanceDatas(now).single()
                .allChildren
                .size,
        )

        assertEquals(
            2,
            getTodayInstanceDatas(now).single()
                .allChildren
                .single()
                .allChildren
                .size,
        )

        assertEquals(0, getTodayInstanceDatas(now, 1).size)
    }

    @Test
    fun testInnerJoinJustTheseInstances() {
        val date = Date(2021, 11, 1)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleTimePair = TimePair(HourMinute(5, 0))

        val repeatingScheduleDatas = listOf(
            ScheduleData.Weekly(DayOfWeek.set, scheduleTimePair, null, null, 1)
        )

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("repeating 1"),
            repeatingScheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("repeating 2"),
            repeatingScheduleDatas,
            null
        )
            .blockingGet()
            .taskKey

        assertEquals(2, getTodayInstanceDatas(now).size)

        assertEquals(2, getTodayInstanceDatas(now, 1).size)

        now += 1.hours

        val singleScheduleDatas = listOf(ScheduleData.Single(date, scheduleTimePair))

        val repeatingInstanceKeys = getTodayInstanceDatas(now).map { it.instanceKey }

        val joinables = repeatingInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) }

        val outerJoinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("outer join"),
            singleScheduleDatas,
            joinables,
            null,
            false,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)

        assertEquals(
            2,
            getTodayInstanceDatas(now).single()
                .allChildren
                .size,
        )

        assertEquals(2, getTodayInstanceDatas(now, 1).size)

        now += 1.hours

        /*
        as-is, this SHOULD result in a tree:

        outer join
        -inner join
        --repeating 1
        --repeating 2

        And should remove the instances for the next day.

        This is as-is, despite the fact that I should support joining instances in this flow (support joinables as param),
        which should give yet another different behavior.
         */
        domainUpdater(now).createJoinChildTask(
            DomainListenerManager.NotificationType.All,
            outerJoinTaskKey,
            EditDelegate.CreateParameters("inner join"),
            repeatingInstanceKeys.map { EditParameters.Join.Joinable.Instance(it) },
            false,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)

        assertEquals(
            1,
            getTodayInstanceDatas(now).single()
                .allChildren
                .size,
        )

        assertEquals(
            2,
            getTodayInstanceDatas(now).single()
                .allChildren
                .single()
                .allChildren
                .size,
        )

        assertEquals(2, getTodayInstanceDatas(now, 1).size)
    }

    @Test
    fun testEditingProjectForRegularSingleSchedule() {
        val date = Date(2021, 11, 16)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val createParameters = EditDelegate.CreateParameters("task")

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0))))

        val taskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            scheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val projectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project",
            setOf(),
        ).blockingGet()

        now += 1.hours

        fun Project<*>.rootTaskIdCount() = projectRecord.rootTaskParentDelegate
            .rootTaskKeys
            .size

        assertEquals(1, domainFactory.projectsFactory.privateProject.rootTaskIdCount())
        assertEquals(0, domainFactory.projectsFactory.sharedProjects.getValue(projectKey).rootTaskIdCount())

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey,
            createParameters,
            scheduleDatas,
            EditDelegate.SharedProjectParameters(projectKey, setOf()),
        ).blockingGet()

        assertEquals(projectKey, domainFactory.getTaskForce(taskKey).project.projectKey)

        assertEquals(0, domainFactory.projectsFactory.privateProject.rootTaskIdCount())
        assertEquals(1, domainFactory.projectsFactory.sharedProjects.getValue(projectKey).rootTaskIdCount())
    }

    @Test
    fun testEditingProjectForMockSingleSchedule() {
        val date = Date(2021, 11, 16)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val createParameters = EditDelegate.CreateParameters("task")

        val taskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val projectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project",
            setOf(),
        ).blockingGet()

        now += 1.hours

        fun Project<*>.rootTaskIdCount() = projectRecord.rootTaskParentDelegate
            .rootTaskKeys
            .size

        assertEquals(1, domainFactory.projectsFactory.privateProject.rootTaskIdCount())
        assertEquals(0, domainFactory.projectsFactory.sharedProjects.getValue(projectKey).rootTaskIdCount())

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey,
            createParameters,
            listOf(ScheduleData.Single(date, TimePair(HourMinute(6, 0)))),
            EditDelegate.SharedProjectParameters(projectKey, setOf()),
        ).blockingGet()

        assertEquals(projectKey, domainFactory.getTaskForce(taskKey).project.projectKey)

        assertEquals(0, domainFactory.projectsFactory.privateProject.rootTaskIdCount())
        assertEquals(1, domainFactory.projectsFactory.sharedProjects.getValue(projectKey).rootTaskIdCount())
    }

    @Test
    fun testAddingExistingInstanceToRecurringInstance() {
        val date = Date(2021, 12, 3)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val singleTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("single task"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(4, 0)))),
            null,
        )
            .blockingGet()
            .taskKey

        val repeatingTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("repeating task"),
            listOf(ScheduleData.Weekly(setOf(DayOfWeek.FRIDAY), TimePair(HourMinute(5, 0)), null, null, 1)),
            null,
        )
            .blockingGet()
            .taskKey

        val instanceDatas = getTodayInstanceDatas(now)

        val singleInstanceKey = instanceDatas.single { it.taskKey == singleTaskKey }.instanceKey
        val repeatingInstanceKey = instanceDatas.single { it.taskKey == repeatingTaskKey }.instanceKey

        now += 1.hours

        fun InstanceKey.setDone(done: Boolean) = domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            this,
            done,
        ).blockingSubscribe()

        singleInstanceKey.setDone(true)
        singleInstanceKey.setDone(false)

        repeatingInstanceKey.setDone(true)
        repeatingInstanceKey.setDone(false)

        now += 1.hours

        getTodayInstanceDatas(now)

        domainUpdater(now).setInstancesParent(
            DomainListenerManager.NotificationType.All,
            setOf(singleInstanceKey),
            repeatingInstanceKey,
        ).blockingSubscribe()

        assertEquals(
            1,
            getTodayInstanceDatas(now).single()
                .allChildren
                .size,
        )
    }

    @Test
    fun testMakeChildTaskTopLevelAndAssignToProject() {
        val date = Date(2021, 12, 20)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0))))

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("parent task"),
            listOf(ScheduleData.Single(date, TimePair(HourMinute(5, 0)))),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val childTaskCreateParameters = EditDelegate.CreateParameters("child task")

        val childTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            childTaskCreateParameters,
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        now += 1.hours

        val projectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "project",
            setOf(),
        ).blockingGet()

        now += 1.hours

        val childTask = domainFactory.getTaskForce(childTaskKey)

        assertEquals(
            domainFactoryRule.domainFactory.projectsFactory.privateProject.projectKey,
            childTask.project.projectKey,
        )

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            childTaskCreateParameters,
            scheduleDatas,
            EditDelegate.SharedProjectParameters(projectKey, setOf()),
        ).blockingGet()

        assertEquals(projectKey, childTask.project.projectKey)
    }

    private fun getSingleScheduleData(date: Date, hour: Int, minute: Int) =
        listOf(ScheduleData.Single(date, TimePair(HourMinute(hour, minute))))

    @Test
    fun testSubchildrenDonePreservedForChildInstanceAfterSettingSchedule() {
        val date = Date(2021, 12, 20)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val parentTaskCreateParameters = EditDelegate.CreateParameters("parent task")

        val parentTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            parentTaskCreateParameters,
            getSingleScheduleData(date, 12, 0),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours

        val middleTaskCreateParameters = EditDelegate.CreateParameters("middle task")

        val middleTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(parentTaskKey),
            middleTaskCreateParameters,
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        now += 1.hours

        val doneChildTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(middleTaskKey),
            EditDelegate.CreateParameters("done child"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        now += 1.hours

        CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(middleTaskKey),
            EditDelegate.CreateParameters("not done child"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        assertTrue(
            getTodayInstanceDatas(now).single()
                .allChildren
                .single()
                .allChildren
                .all { it.done == null }
        )

        now += 1.hours

        val doneInstance =
            domainFactory.getTaskForce(doneChildTaskKey).getInstances(null, null, now).single()

        domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            doneInstance.instanceKey,
            true,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).single()
            .allChildren
            .single()
            .allChildren
            .let {
                assertEquals(1, it.filter { it.done == null }.size)
                assertEquals(1, it.filter { it.done != null }.size)
            }

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            parentTaskCreateParameters,
            getSingleScheduleData(date, 13, 0),
            null,
        ).blockingGet()

        getTodayInstanceDatas(now).single()
            .allChildren
            .single()
            .allChildren
            .let {
                assertEquals(1, it.filter { it.done == null }.size)
                assertEquals(1, it.filter { it.done != null }.size)
            }

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            middleTaskKey,
            middleTaskCreateParameters,
            getSingleScheduleData(date, 14, 0),
            null,
        ).blockingGet()

        getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)

            assertTrue(it[0].allChildren.isEmpty())

            val middleInstanceChildren = it[1].allChildren

            assertEquals(1, middleInstanceChildren.filter { it.done == null }.size)
            assertEquals(1, middleInstanceChildren.filter { it.done != null }.size)
        }
    }

    @Test
    fun testSubchildrenDonePreservedForChildInstanceAfterJoiningThenSettingSchedule() {
        val date = Date(2021, 12, 20)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val singleMiddleTaskCreateParameters = EditDelegate.CreateParameters("single middle task")

        val singleMiddleTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            singleMiddleTaskCreateParameters,
            getSingleScheduleData(date, 12, 0),
            null,
        )
            .blockingGet()
            .taskKey

        now += 1.hours // 2 AM

        val weeklyMiddleTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("weekly middle task"),
            getSingleScheduleData(date, 12, 0),
            null,
        )
            .blockingGet()
            .taskKey

        assertEquals(2, getTodayInstanceDatas(now).size)

        now += 1.hours

        val doneChildTaskKey = CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(singleMiddleTaskKey),
            EditDelegate.CreateParameters("done child"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        now += 1.hours

        /*val notDoneChildTaskKey = */CreateChildTaskDomainUpdate(
            DomainListenerManager.NotificationType.All,
            CreateChildTaskDomainUpdate.Parent.Task(singleMiddleTaskKey),
            EditDelegate.CreateParameters("not done child"),
        ).perform(domainUpdater(now))
            .blockingGet()
            .taskKey

        assertTrue(
            getTodayInstanceDatas(now).single { it.taskKey == singleMiddleTaskKey }
                .allChildren
                .all { it.done == null }
        )

        now += 1.hours

        val doneInstance =
            domainFactory.getTaskForce(doneChildTaskKey).getInstances(null, null, now).single()

        domainUpdater(now).setInstanceDone(
            DomainListenerManager.NotificationType.All,
            doneInstance.instanceKey,
            true,
        ).blockingSubscribe()

        getTodayInstanceDatas(now).single { it.taskKey == singleMiddleTaskKey }
            .allChildren
            .let {
                assertEquals(1, it.filter { it.done == null }.size)
                assertEquals(1, it.filter { it.done != null }.size)
            }

        now += 1.hours

        val parentTaskCreateParameters = EditDelegate.CreateParameters("parent task")

        val parentTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            parentTaskCreateParameters,
            getSingleScheduleData(date, 13, 0),
            getTodayInstanceDatas(now).map { EditParameters.Join.Joinable.Instance(it.instanceKey) },
            null,
            false,
        ).blockingGet()

        assertEquals(1, getTodayInstanceDatas(now).size)

        getTodayInstanceDatas(now).single()
            .allChildren
            .single { it.taskKey == singleMiddleTaskKey }
            .allChildren
            .let {
                assertEquals(1, it.filter { it.done == null }.size)
                assertEquals(1, it.filter { it.done != null }.size)
            }

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            parentTaskKey,
            parentTaskCreateParameters,
            getSingleScheduleData(date, 14, 0),
            null,
        ).blockingGet()

        getTodayInstanceDatas(now).single()
            .allChildren
            .single { it.taskKey == singleMiddleTaskKey }
            .allChildren
            .let {
                assertEquals(1, it.filter { it.done == null }.size)
                assertEquals(1, it.filter { it.done != null }.size)
            }

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            singleMiddleTaskKey,
            singleMiddleTaskCreateParameters,
            getSingleScheduleData(date, 15, 0),
            null,
        ).blockingGet()

        getTodayInstanceDatas(now).let {
            assertEquals(2, it.size)

            assertTrue(it[0].allChildren.single().taskKey == weeklyMiddleTaskKey)

            val middleInstanceChildren = it[1].allChildren

            assertEquals(1, middleInstanceChildren.filter { it.done == null }.size)
            assertEquals(1, middleInstanceChildren.filter { it.done != null }.size)
        }
    }
}