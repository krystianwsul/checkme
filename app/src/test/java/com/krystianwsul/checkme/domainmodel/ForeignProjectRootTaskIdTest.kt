package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.extensions.createProject
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleJoinTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import com.soywiz.klock.hours
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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

    @Test
    fun testSingleInstanceJoin() {
        val date = Date(2022, 5, 30)
        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val sharedProjectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "shared project",
            setOf(),
        ).blockingGet()

        now += 1.hours

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(10, 0)))

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

        Notifier.setIrrelevant(domainFactory, now)
    }
}