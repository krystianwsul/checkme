package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.days
import com.soywiz.klock.hours
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY, false)
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
}