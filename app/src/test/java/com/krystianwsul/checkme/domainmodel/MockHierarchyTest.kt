package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesParent
import com.krystianwsul.checkme.domainmodel.extensions.updateScheduleTask
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.hours
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MockHierarchyTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    private fun getTodayInstanceDatas(now: ExactTimeStamp.Local) =
        domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
            .groupListDataWrapper
            .allInstanceDatas

    private fun getSingleScheduleData(date: Date, hour: Int, minute: Int) =
        listOf(ScheduleData.Single(date, TimePair(HourMinute(hour, minute))))

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

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            childTaskKey,
            EditDelegate.CreateParameters("child task"),
            singleScheduleData,
            null,
        ).blockingSubscribe()

        assertEquals(2, getTodayInstanceDatas(now).size)
    }
}