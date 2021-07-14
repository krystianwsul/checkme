package com.krystianwsul.common.firebase.models.task

import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.TestDomainUpdater
import com.krystianwsul.checkme.domainmodel.extensions.createProject
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.TaskKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectRootTaskIdTrackerTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    @Test
    fun testParentInstance() {
        val date = Date(2021, 7, 14)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val sharedProjectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "shared project",
            emptySet(),
        ).blockingGet()

        val scheduleTimePair = TimePair(HourMinute(5, 0))

        val privateWeeklyTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "private weekly",
            listOf(ScheduleData.Weekly(setOf(DayOfWeek.WEDNESDAY), scheduleTimePair, null, null, 1)),
            null,
            null,
            null,
        )
            .blockingGet()
            .taskKey

        val sharedSingleTaskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            "shared single",
            listOf(ScheduleData.Single(date, scheduleTimePair)),
            null,
            EditDelegate.SharedProjectParameters(sharedProjectKey, emptySet()),
            null,
        )
            .blockingGet()
            .taskKey

        val privateProject = domainFactory.projectsFactory.privateProject
        val sharedProject = domainFactory.projectsFactory.getProjectForce(sharedProjectKey)

        val privateTask = domainFactory.rootTasksFactory.getRootTask(privateWeeklyTaskKey)
        val sharedTask = domainFactory.rootTasksFactory.getRootTask(sharedSingleTaskKey)

        fun Project<*>.rootTaskKeys() = projectRecord.rootTaskParentDelegate.rootTaskKeys
        fun RootTask.rootTaskKeys() = taskRecord.rootTaskParentDelegate.rootTaskKeys

        assertEquals(setOf(privateWeeklyTaskKey), privateProject.rootTaskKeys())
        assertEquals(setOf(sharedSingleTaskKey), sharedProject.rootTaskKeys())

        assertEquals(emptySet<TaskKey.Root>(), privateTask.rootTaskKeys())
        assertEquals(emptySet<TaskKey.Root>(), sharedTask.rootTaskKeys())
    }
}