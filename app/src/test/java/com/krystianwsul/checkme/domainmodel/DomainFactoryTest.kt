package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.soywiz.klock.hours
import org.junit.Assert.assertEquals
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
                SaveService.Source.SERVICE,
                "task",
                listOf(ScheduleData.Single(Date(2020, 12, 20), TimePair(HourMinute(20, 0)))),
                null,
                null,
                null
        )

        assertEquals(
                "task",
                domainFactory.getMainData()
                        .taskData
                        .childTaskDatas
                        .single()
                        .name
        )
    }

    @Test
    fun testCircularDependencyInChildIntervals() {
        val date = Date(2020, 12, 21)
        var now = ExactTimeStamp.Local(date, HourMinute(0, 0))

        val scheduleDatas = listOf(ScheduleData.Single(date, TimePair(HourMinute(10, 0))))

        val taskName1 = "task1"
        val taskKey1 = domainFactory.createScheduleRootTask(
                SaveService.Source.SERVICE,
                taskName1,
                scheduleDatas,
                null,
                null,
                null,
                now = now,
        )

        now += 1.hours

        val taskName2 = "task2"
        val taskKey2 = domainFactory.createChildTask(
                SaveService.Source.SERVICE,
                taskKey1,
                taskName2,
                null,
                null,
                now = now
        )

        fun getInstanceDatas() = domainFactory.getGroupListData(now, 0, Preferences.TimeRange.DAY)
                .groupListDataWrapper
                .instanceDatas

        assertEquals(taskKey1, getInstanceDatas().single().taskKey)
        assertEquals(taskKey2, getInstanceDatas().single().children.values.single().taskKey)

        now += 1.hours

        domainFactory.updateScheduleTask(
                SaveService.Source.SERVICE,
                taskKey2,
                taskName2,
                scheduleDatas,
                null,
                null,
                null,
                now,
        )

        assertEquals(2, getInstanceDatas().size)

        now += 1.hours

        domainFactory.updateChildTask(
                SaveService.Source.SERVICE,
                taskKey1,
                taskName1,
                taskKey2,
                null,
                null,
                null,
                true,
                now,
        )

        domainFactory.getTaskForce(taskKey1).invalidateIntervals()
        domainFactory.getTaskForce(taskKey2).invalidateIntervals()

        assertEquals(taskKey2, getInstanceDatas().single().taskKey)
        assertEquals(taskKey1, getInstanceDatas().single().children.values.single().taskKey)
    }
}