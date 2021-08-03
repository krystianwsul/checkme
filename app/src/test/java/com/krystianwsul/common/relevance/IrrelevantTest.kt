package com.krystianwsul.common.relevance

import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.roottask.RootTaskKeySource
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksLoader
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.schedule.*
import com.krystianwsul.common.firebase.json.taskhierarchies.NestedTaskHierarchyJson
import com.krystianwsul.common.firebase.json.taskhierarchies.ProjectTaskHierarchyJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.task.*
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class IrrelevantTest {

    companion object {

        private val userKey = UserKey("key")

        private val userInfo = spyk(UserInfo("email", "name", "uid")) {
            every { key } returns userKey
        }

        private val databaseWrapper = mockk<DatabaseWrapper> {
            every { newRootTaskRecordId() } returns "taskRecordId"
            every { newRootTaskNoScheduleOrParentRecordId(any()) } returns "noScheduleOrParentRecordId"
        }

        private val shownFactory = mockk<Instance.ShownFactory> {
            every { getShown(any()) } returns mockk(relaxed = true)
        }

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    private val compositeDisposable = CompositeDisposable()

    @Before
    fun before() {
        ProjectRootTaskIdTracker.instance = null
    }

    @After
    fun after() {
        compositeDisposable.clear()
    }

    private fun <T> mockProjectRootTaskIdTracker(action: () -> T): T {
        check(ProjectRootTaskIdTracker.instance == null)
        ProjectRootTaskIdTracker.instance = mockk()

        val result = action()

        checkNotNull(ProjectRootTaskIdTracker.instance)
        ProjectRootTaskIdTracker.instance = null

        return result
    }

    @Test
    fun testDisappearingTask() {
        // 1: create task with single schedule

        val day1 = Date(2020, 1, 1)
        val day2 = Date(2020, 1, 2)
        val hour1 = HourMinute(1, 1).toHourMilli()
        val hour2 = HourMinute(2, 1).toHourMilli()
        val hour3 = HourMinute(3, 1)
        val hour4 = HourMinute(4, 1)

        var now = ExactTimeStamp.Local(day1, hour1)

        val projectJson = PrivateProjectJson(startTime = now.long)
        val projectRecord = PrivateProjectRecord(databaseWrapper, userInfo, projectJson)

        val rootTasksManager = AndroidRootTasksManager(databaseWrapper)

        val rootTaskKeySource = mockk<RootTaskKeySource> {
            every { rootTaskKeysObservable } returns Observable.just(setOf())
            every { onProjectAddedOrUpdated(any(), any()) } returns Unit
        }

        val rootTaskLoader = RootTasksLoader(
            rootTaskKeySource,
            mockk {
                every { getRootTaskObservable(any()) } returns Observable.never()
            },
            compositeDisposable,
            rootTasksManager,
        )

        lateinit var project: PrivateProject

        val projectsFactory = mockk<ProjectsFactory> {
            every { getProjectForce(any()) } answers { project }
            every { projects } answers { mapOf(project.projectKey to project) }
        }

        val rootModelChangeManager = RootModelChangeManager()

        val rootTasksFactory = RootTasksFactory(
            rootTaskLoader,
            mockk(),
            mockk {
                every { getDependencies(any()) } returns mockk()
            },
            compositeDisposable,
            rootTaskKeySource,
            rootModelChangeManager,
        ) { projectsFactory }

        project = PrivateProject(projectRecord, mockk(), rootTasksFactory, rootModelChangeManager)

        now = ExactTimeStamp.Local(day1, hour2)

        val scheduleWrapper = RootScheduleWrapper(
            singleScheduleJson = RootSingleScheduleJson(
                startTime = now.long,
                startTimeOffset = now.offset,
                year = day1.year,
                month = day1.month,
                day = day1.day,
                time = hour3.toJson(),
            )
        )

        val taskJson = RootTaskJson(
            name = "task",
            startTime = now.long,
            startTimeOffset = now.offset,
            schedules = mutableMapOf("scheduleKey" to scheduleWrapper),
        )

        val task = mockProjectRootTaskIdTracker { rootTasksFactory.newTask(taskJson) }

        // 2: once reminded, add one hour

        now = ExactTimeStamp.Local(day1, hour3)

        val instance = task.getInstances(
            null,
            now.toOffset().plusOne(),
            now,
            onlyRoot = true,
        ).single()

        instance.setInstanceDateTime(shownFactory, DateTime(day1, Time.Normal(hour4)), mockk(), now)

        // 3: after second reminder, remove schedule, then set reminder done

        now = ExactTimeStamp.Local(day1, hour4.toHourMilli())

        mockProjectRootTaskIdTracker {
            task.performRootIntervalUpdate {
                endAllCurrentTaskHierarchies(now)
                endAllCurrentSchedules(now)
                endAllCurrentNoScheduleOrParents(now)

                setNoScheduleOrParent(now, project.projectKey)
            }
        }

        instance.setDone(shownFactory, true, now)

        fun Task.isReminderless() = notDeleted &&
                this.isVisible(now, true) &&
                isTopLevelTask(now) &&
                intervalInfo.getCurrentScheduleIntervals(now).isEmpty()

        assertTrue(task.isReminderless())

        // 4: next day, task should still be reminderless, instead of ending up with expired schedule again

        now = ExactTimeStamp.Local(day2, hour1)

        Irrelevant.setIrrelevant(
            { rootTasksFactory.rootTasks },
            mapOf(),
            { mapOf(project.projectKey to project) },
            rootTasksFactory,
            now,
        )

        assertTrue(task.isReminderless())
    }

    @Test
    fun testSingleScheduleChangedToRepeating() {
        // 1. Create task with single schedule and repeating schedule

        val day1 = Date(2020, 10, 6) // tuesday
        val day2 = Date(2020, 10, 7) // wednesday
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0).toHourMilli()
        val hour3 = HourMinute(3, 0).toHourMilli()

        var now = ExactTimeStamp.Local(day1, hour1)

        val singleScheduleWrapper = PrivateScheduleWrapper(
            singleScheduleJson = PrivateSingleScheduleJson(
                startTime = now.long,
                year = day1.year,
                month = day1.month,
                day = day1.day,
                hour = hour2.hour,
                minute = hour2.minute,
            )
        )

        val weeklyScheduleWrapper = PrivateScheduleWrapper(
            weeklyScheduleJson = PrivateWeeklyScheduleJson(
                startTime = now.long,
                dayOfWeek = 1, // monday
                hour = hour1.hour,
                minute = hour1.minute,
            )
        )

        val taskJson = PrivateTaskJson(
            name = "task",
            startTime = now.long,
            schedules = mutableMapOf(
                "singleScheduleKey" to singleScheduleWrapper,
                "weeklyScheduleKey" to weeklyScheduleWrapper,
            ),
        )

        val projectKey = ProjectKey.Private(userKey.key)

        val taskId = "taskKey"

        val projectJson = PrivateProjectJson(
            startTime = now.long,
            tasks = mutableMapOf(taskId to taskJson),
        )

        val existingInstanceChangeManager = RootModelChangeManager()

        val projectRecord = PrivateProjectRecord(databaseWrapper, projectKey, projectJson)
        val project = PrivateProject(projectRecord, mockk(), mockk(relaxed = true), existingInstanceChangeManager)
        val task = project.projectTasks.single()

        // 2. Mark single instance done

        assertTrue(task.intervalInfo.getCurrentScheduleIntervals(now).size == 2)

        now = ExactTimeStamp.Local(day1, hour2)

        val instance = task.getInstances(
            null,
            now.toOffset().plusOne(),
            now,
            onlyRoot = true,
        ).single()

        instance.setDone(shownFactory, true, now)
        projectRecord.getValues(mutableMapOf())

        // 3. Check both instance and schedule removed next day

        now = ExactTimeStamp.Local(day2, hour3)

        assertFalse(
            task.getInstances(
                null,
                now.toOffset().plusOne(),
                now,
                onlyRoot = true,
            )
                .single()
                .isVisible(now, Instance.VisibilityOptions(hack24 = true))
        )
        assertTrue(task.intervalInfo.getCurrentScheduleIntervals(now).size == 2)

        Irrelevant.setIrrelevant(
            { mapOf() },
            mapOf(),
            { mapOf(project.projectKey to project) },
            newMockRootTaskProvider(),
            now,
        )

        assertTrue(task.intervalInfo.getCurrentScheduleIntervals(now).size == 1)
        assertTrue(
            task.getInstances(
                null,
                now.toOffset().plusOne(),
                now,
                onlyRoot = true,
            )
                .toList()
                .isEmpty()
        )
    }

    @Test
    fun testDeletedChildTaskIsntInInstanceProjectTaskHierarchy() {
        // 1: Create task with single schedule and single child
        // 2: Delete single child with remove instances option
        // 3: Add new child
        // 4: Reschedule instance
        // 5: check instance has only the second child

        val day1 = Date(2020, 10, 6) // tuesday
        val day2 = Date(2020, 10, 7) // wednesday
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0).toHourMilli()
        val hour3 = HourMinute(3, 0).toHourMilli()
        val hour4 = HourMinute(4, 0).toHourMilli()
        val hour5 = HourMinute(5, 0).toHourMilli()

        var now = ExactTimeStamp.Local(day1, hour1)

        val projectKey = ProjectKey.Private(userKey.key)

        val singleScheduleWrapper = PrivateScheduleWrapper(
            singleScheduleJson = PrivateSingleScheduleJson(
                startTime = now.long,
                year = day1.year,
                month = day1.month,
                day = day1.day,
                hour = hour2.hour,
                minute = hour2.minute,
            )
        )

        val parentTaskJson = PrivateTaskJson(
            name = "parentTask",
            startTime = now.long,
            schedules = mutableMapOf("singleScheduleKey" to singleScheduleWrapper),
        )
        val parentTaskId = "parentTaskKey"

        val child1TaskJson = PrivateTaskJson(
            name = "child1Task",
            startTime = now.long,
        )
        val child1TaskId = "child1TaskKey"
        val taskHierarchy1Json = ProjectTaskHierarchyJson(
            parentTaskId = parentTaskId,
            childTaskId = child1TaskId,
            startTime = now.long,
        )
        val taskHierarchy1Id = "taskHierarchy1"

        val child2TaskJson = PrivateTaskJson(
            name = "child2Task",
            startTime = now.long,
        )
        val child2TaskId = "child2TaskKey"
        val taskHierarchy2Json = ProjectTaskHierarchyJson(
            parentTaskId = parentTaskId,
            childTaskId = child2TaskId,
            startTime = now.long,
        )
        val taskHierarchy2Id = "taskHierarchy2"

        val projectJson = PrivateProjectJson(
            startTime = now.long,
            tasks = mutableMapOf(
                parentTaskId to parentTaskJson,
                child1TaskId to child1TaskJson,
                child2TaskId to child2TaskJson,
            ),
            taskHierarchies = mutableMapOf(
                taskHierarchy1Id to taskHierarchy1Json,
                taskHierarchy2Id to taskHierarchy2Json,
            ),
        )

        val existingInstanceChangeManager = RootModelChangeManager()

        val projectRecord = PrivateProjectRecord(databaseWrapper, projectKey, projectJson)
        val project = PrivateProject(projectRecord, mockk(), mockk(relaxed = true), existingInstanceChangeManager)

        val parentTask = project.projectTasks.single { it.isTopLevelTask(now) }
        assertEquals(2, parentTask.getChildTaskHierarchies(now).size)

        val child1Task = parentTask.getChildTaskHierarchies(now)
            .single { it.childTaskId == child1TaskId }
            .childTask

        val child2Task = parentTask.getChildTaskHierarchies(now)
            .single { it.childTaskId == child2TaskId }
            .childTask

        now = ExactTimeStamp.Local(day1, hour2)

        val parentInstance = parentTask.getInstances(
            null,
            now.toOffset().plusOne(),
            now,
            onlyRoot = true
        ).single()
        assertEquals(2, parentInstance.getChildInstances().size)

        val child1Instance = parentInstance.getChildInstances().single {
            (it.instanceKey
                .taskKey as TaskKey.Project)
                .taskId == child1TaskId
        }

        child1Instance.setDone(shownFactory, true, now)

        now = ExactTimeStamp.Local(day1, hour3)

        child1Task.performIntervalUpdate { setEndData(Task.EndData(now, true)) }
        child2Task.performIntervalUpdate { setEndData(Task.EndData(now, true)) }

        now = ExactTimeStamp.Local(day1, hour4)

        assertTrue(parentTask.getChildTaskHierarchies(now).isEmpty())
        assertTrue(
            parentInstance.getChildInstances().single {
                it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true))
            } == child1Instance
        )

        now = ExactTimeStamp.Local(day2, hour5)

        Irrelevant.setIrrelevant(
            { mapOf() },
            mapOf(),
            { mapOf(project.projectKey to project) },
            newMockRootTaskProvider(),
            now,
        )
    }

    @Test
    fun testDeletedChildTaskIsntInInstanceNestedTaskHierarchy() {
        // 1: Create task with single schedule and single child
        // 2: Delete single child with remove instances option
        // 3: Add new child
        // 4: Reschedule instance
        // 5: check instance has only the second child

        val day1 = Date(2020, 10, 6) // tuesday
        val day2 = Date(2020, 10, 7) // wednesday
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0).toHourMilli()
        val hour3 = HourMinute(3, 0).toHourMilli()
        val hour4 = HourMinute(4, 0).toHourMilli()
        val hour5 = HourMinute(5, 0).toHourMilli()

        var now = ExactTimeStamp.Local(day1, hour1)

        val projectKey = ProjectKey.Private(userKey.key)

        val singleScheduleWrapper = PrivateScheduleWrapper(
            singleScheduleJson = PrivateSingleScheduleJson(
                startTime = now.long,
                startTimeOffset = now.offset,
                year = day1.year,
                month = day1.month,
                day = day1.day,
                hour = hour2.hour,
                minute = hour2.minute,
            )
        )

        val parentTaskJson = PrivateTaskJson(
            name = "parentTask",
            startTime = now.long,
            startTimeOffset = now.offset,
            schedules = mutableMapOf("singleScheduleKey" to singleScheduleWrapper),
        )
        val parentTaskId = "parentTaskKey"

        val taskHierarchy1Json = NestedTaskHierarchyJson(
            parentTaskId = parentTaskId,
            startTime = now.long,
            startTimeOffset = now.offset,
        )
        val taskHierarchy1Id = "taskHierarchy1"

        val child1TaskJson = PrivateTaskJson(
            name = "child1Task",
            startTime = now.long,
            startTimeOffset = now.offset,
            taskHierarchies = mapOf(taskHierarchy1Id to taskHierarchy1Json),
        )
        val child1TaskId = "child1TaskKey"

        val taskHierarchy2Json = NestedTaskHierarchyJson(
            parentTaskId = parentTaskId,
            startTime = now.long,
            startTimeOffset = now.offset,
        )
        val taskHierarchy2Id = "taskHierarchy2"

        val child2TaskJson = PrivateTaskJson(
            name = "child2Task",
            startTime = now.long,
            startTimeOffset = now.offset,
            taskHierarchies = mapOf(taskHierarchy2Id to taskHierarchy2Json),
        )
        val child2TaskId = "child2TaskKey"

        val projectJson = PrivateProjectJson(
            startTime = now.long,
            startTimeOffset = now.offset,
            tasks = mutableMapOf(
                parentTaskId to parentTaskJson,
                child1TaskId to child1TaskJson,
                child2TaskId to child2TaskJson,
            ),
        )

        val existingInstanceChangeManager = RootModelChangeManager()

        val projectRecord = PrivateProjectRecord(databaseWrapper, projectKey, projectJson)
        val project = PrivateProject(projectRecord, mockk(), mockk(relaxed = true), existingInstanceChangeManager)

        val parentTask = project.projectTasks.single { it.isTopLevelTask(now) }
        assertEquals(2, parentTask.getChildTaskHierarchies(now).size)

        val child1Task = parentTask.getChildTaskHierarchies(now)
            .single { it.childTaskId == child1TaskId }
            .childTask

        val child2Task = parentTask.getChildTaskHierarchies(now)
            .single { it.childTaskId == child2TaskId }
            .childTask

        now = ExactTimeStamp.Local(day1, hour2)

        val parentInstance = parentTask.getInstances(
            null,
            now.toOffset().plusOne(),
            now,
            onlyRoot = true,
        ).single()
        assertEquals(2, parentInstance.getChildInstances().size)

        val child1Instance = parentInstance.getChildInstances().single {
            (it.instanceKey
                .taskKey as TaskKey.Project)
                .taskId == child1TaskId
        }

        child1Instance.setDone(shownFactory, true, now)

        now = ExactTimeStamp.Local(day1, hour3)

        child1Task.performIntervalUpdate { setEndData(Task.EndData(now, true)) }
        child2Task.performIntervalUpdate { setEndData(Task.EndData(now, true)) }

        now = ExactTimeStamp.Local(day1, hour4)

        assertTrue(parentTask.getChildTaskHierarchies(now).isEmpty())
        assertTrue(
            parentInstance.getChildInstances().single {
                it.isVisible(now, Instance.VisibilityOptions(assumeChildOfVisibleParent = true))
            } == child1Instance
        )

        now = ExactTimeStamp.Local(day2, hour5)

        Irrelevant.setIrrelevant(
            { mapOf() },
            mapOf(),
            { mapOf(project.projectKey to project) },
            newMockRootTaskProvider(),
            now,
        )
    }

    private fun newMockRootTaskProvider() = mockk<Project.RootTaskProvider>()

    @Test
    fun testRepeatingScheduleDelete() {
        // 1. Create task with repeating schedule

        val day1 = Date(2021, 7, 20) // tuesday
        val day2 = Date(2021, 7, 21) // wednesday
        val hour1 = HourMinute(1, 0).toHourMilli()
        val hour2 = HourMinute(2, 0)
        val hour3 = HourMinute(3, 0).toHourMilli()
        val hour4 = HourMinute(4, 0).toHourMilli()

        // day1, hour1
        var now = ExactTimeStamp.Local(day1, hour1)

        val weeklyScheduleWrapper = RootScheduleWrapper(
            weeklyScheduleJson = RootWeeklyScheduleJson(
                startTime = now.long,
                dayOfWeek = DayOfWeek.TUESDAY.ordinal,
                time = JsonTime.Normal(hour2).toJson(),
                projectId = userKey.key,
            )
        )

        val taskJson = RootTaskJson(
            name = "task",
            startTime = now.long,
            schedules = mutableMapOf("weeklyScheduleKey" to weeklyScheduleWrapper),
        )

        val taskId = "taskKey"
        val taskRecord = RootTaskRecord(taskId, taskJson, mockk(), mockk())

        lateinit var task: RootTask
        lateinit var project: PrivateProject

        var taskDeleted = false

        val existingInstanceChangeManager = RootModelChangeManager()

        val taskParent = mockk<RootTask.Parent> {
            every { rootModelChangeManager } returns existingInstanceChangeManager

            every { getRootTasksForProject(any()) } answers {
                if (taskDeleted)
                    emptyList()
                else
                    listOf(task)
            }

            every { getProject(any()) } answers { project }

            every { getTaskHierarchiesByParentTaskKey(any()) } returns setOf()

            every { getAllExistingInstances() } answers {
                if (taskDeleted)
                    emptySequence()
                else
                    task.existingInstances.values.asSequence()
            }

            every { deleteRootTask(any()) } answers {
                check(!taskDeleted)

                taskDeleted = true
            }

            every { updateProjectRecord(any(), any()) } returns Unit
        }

        task = RootTask(taskRecord, taskParent, mockk())

        val projectKey = ProjectKey.Private(userKey.key)
        val projectJson = PrivateProjectJson(startTime = now.long)

        val projectRecord = PrivateProjectRecord(databaseWrapper, projectKey, projectJson)
        project = PrivateProject(projectRecord, mockk(), taskParent, existingInstanceChangeManager)

        // 2. Mark today's instance done

        assertTrue(task.intervalInfo.getCurrentScheduleIntervals(now).size == 1)

        // day1, hour2
        now = ExactTimeStamp.Local(day1, hour2)

        val instance = task.getInstances(
            null,
            now.toOffset().plusOne(),
            now,
            onlyRoot = true,
        ).single()

        instance.setDone(shownFactory, true, now)
        projectRecord.getValues(mutableMapOf())

        // 3. Delete the task

        // day1, hour3
        now = ExactTimeStamp.Local(day1, hour3)

        task.performIntervalUpdate { setEndData(Task.EndData(now, true)) }

        fun setIrrelevant(now: ExactTimeStamp.Local) = Irrelevant.setIrrelevant(
            {
                if (taskDeleted)
                    emptyMap()
                else
                    mapOf(taskRecord.taskKey to task)
            },
            mapOf(),
            { mapOf(project.projectKey to project) },
            taskParent,
            now,
        )

        val result1 = setIrrelevant(now)

        assertTrue(result1.irrelevantExistingInstances.isEmpty())
        assertTrue(result1.irrelevantTasks.isEmpty())

        // 4. Check everything works after 23 hours

        now = ExactTimeStamp.Local(day2, hour1)

        assertTrue(
            task.getInstances(
                null,
                now.toOffset().plusOne(),
                now,
                onlyRoot = true,
            )
                .toList()
                .isNotEmpty()
        )

        taskRecord.getValues(mutableMapOf()) // to clear update flags in records

        setIrrelevant(now).let {
            assertTrue(it.irrelevantExistingInstances.isEmpty())
            assertTrue(it.irrelevantTasks.isEmpty())
        }

        assertEquals(projectKey, task.project.projectKey)

        assertTrue(task.intervalInfo.scheduleIntervals.size == 1)

        // 5. Check task removed 25 hours later

        now = ExactTimeStamp.Local(day2, hour4)

        assertTrue(setIrrelevant(now).irrelevantTasks.isNotEmpty())
    }
}