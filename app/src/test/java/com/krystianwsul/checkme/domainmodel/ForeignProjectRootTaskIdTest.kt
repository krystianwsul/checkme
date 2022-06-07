package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.extensions.createProject
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.firebase.models.checkInconsistentRootTaskIds
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import com.soywiz.klock.hours
import org.junit.*
import org.junit.Assert.assertTrue

class ForeignProjectRootTaskIdTest {

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

    private fun Project<*>.containsRootTaskId(taskKey: TaskKey.Root) =
        taskKey in projectRecord.rootTaskParentDelegate.rootTaskKeys

    private fun createSharedProject(now: ExactTimeStamp.Local) = domainUpdater(now).createProject(
        DomainListenerManager.NotificationType.All,
        "shared project",
        setOf(),
    ).blockingGet()

    private fun singleScheduleData(date: Date) = listOf(ScheduleData.Single(date, TimePair(10, 0)))

    private val privateProject get() = domainFactory.projectsFactory.privateProject

    @Ignore("todo join test")
    @Test
    fun testSingleInstanceJoin() {
        val date = Date(2022, 5, 30)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val sharedProjectKey = createSharedProject(now)

        now += 1.hours

        val scheduleDatas = singleScheduleData(date)

        val childTaskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task 1"),
            scheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        val privateProject = domainFactory.projectsFactory.privateProject

        assertTrue(privateProject.containsRootTaskId(childTaskKey1))

        now += 1.hours

        val childTaskKey2 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task 2"),
            scheduleDatas,
            null,
        )
            .blockingGet()
            .taskKey

        assertTrue(privateProject.containsRootTaskId(childTaskKey2))

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("join task"),
            scheduleDatas,
            listOf(childTaskKey1, childTaskKey2).map {
                domainFactory.getTaskForce(it)
                    .getInstances(null, null, now)
                    .single()
                    .instanceKey
                    .let { EditParameters.Join.Joinable.Instance(it) }
            },
            EditDelegate.ProjectParameters(sharedProjectKey, setOf()),
            false,
        ).blockingGet()

        val sharedProject = domainFactory.getProjectForce(sharedProjectKey)

        assertTrue(sharedProject.containsRootTaskId(joinTaskKey))
        assertTrue(sharedProject.containsRootTaskId(childTaskKey1))
        assertTrue(sharedProject.containsRootTaskId(childTaskKey2))

        assertTrue(privateProject.projectRecord.rootTaskParentDelegate.rootTaskKeys.isEmpty())

        checkInconsistentRootTaskIds(
            domainFactory.rootTasksFactory.getRootTasks(),
            domainFactory.projectsFactory.projects.values
        )
    }

    @Test
    fun testWeeklyTaskJoin() {
        val date = Date(2022, 5, 31)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val sharedProjectKey = createSharedProject(now)

        now += 1.hours

        val weeklyScheduleData = listOf(ScheduleData.Weekly(setOf(DayOfWeek.TUESDAY), TimePair(10, 0), null, null, 1))

        val childTaskKey1 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task 1"),
            weeklyScheduleData,
            null,
        )
            .blockingGet()
            .taskKey

        val privateProject = domainFactory.projectsFactory.privateProject

        assertTrue(privateProject.containsRootTaskId(childTaskKey1))

        now += 1.hours

        val childTaskKey2 = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("child task 2"),
            weeklyScheduleData,
            null,
        )
            .blockingGet()
            .taskKey

        assertTrue(privateProject.containsRootTaskId(childTaskKey2))

        now += 1.hours

        val joinTaskKey = domainUpdater(now).createScheduleJoinTopLevelTask(
            DomainListenerManager.NotificationType.All,
            EditDelegate.CreateParameters("join task"),
            singleScheduleData(date),
            listOf(childTaskKey1, childTaskKey2).map {
                domainFactory.getTaskForce(it)
                    .getInstances(null, null, now)
                    .first()
                    .instanceKey
                    .let { EditParameters.Join.Joinable.Instance(it) }
            },
            EditDelegate.ProjectParameters(sharedProjectKey, setOf()),
            false,
        ).blockingGet()

        val sharedProject = domainFactory.getProjectForce(sharedProjectKey)

        assertTrue(sharedProject.containsRootTaskId(joinTaskKey))
        assertTrue(sharedProject.containsRootTaskId(childTaskKey1))
        assertTrue(sharedProject.containsRootTaskId(childTaskKey2))

        assertTrue(privateProject.containsRootTaskId(joinTaskKey))
        assertTrue(privateProject.containsRootTaskId(childTaskKey1))
        assertTrue(privateProject.containsRootTaskId(childTaskKey2))

        checkInconsistentRootTaskIds(
            domainFactory.rootTasksFactory.getRootTasks(),
            domainFactory.projectsFactory.projects.values
        )
    }
}