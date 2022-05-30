package com.krystianwsul.checkme.domainmodel

import android.util.Base64
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.createProject
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleTopLevelTask
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.extensions.updateScheduleTask
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.common.firebase.ReasonWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.models.users.RootUser
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.UserKey
import com.soywiz.klock.hours
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AssignedToTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val domainFactory get() = domainFactoryRule.domainFactory

    private fun domainUpdater(now: ExactTimeStamp.Local = ExactTimeStamp.Local.now) =
        TestDomainUpdater(domainFactory, now)

    private fun getDayInstanceDatas(now: ExactTimeStamp.Local, day: Int = 0) =
        domainFactory.getGroupListData(now, day, false, Preferences.ProjectFilter.All)
            .groupListDataWrapper
            .allInstanceDatas

    private fun getSingleScheduleData(date: Date, hour: Int, minute: Int) =
        listOf(ScheduleData.Single(date, TimePair(HourMinute(hour, minute))))

    private val friendJson = UserJson("friend@email.com", "friend name")

    private val friendKey = UserKey("friendKey")
    private val friendRootUser = mockk<RootUser>()

    @Before
    fun before() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns friendKey.key

        domainFactoryRule.friendsFactory.apply {
            every { userMap } returns mutableMapOf(friendKey to ReasonWrapper(UserLoadReason.FRIEND, friendRootUser))
            every { getUserJsons(any()) } returns mapOf(friendKey to friendJson)
        }
    }

    @Test
    fun test() {
        val date = Date(2022, 5, 3)

        var now = ExactTimeStamp.Local(date, HourMinute(1, 0))

        val sharedProjectKey = domainUpdater(now).createProject(
            DomainListenerManager.NotificationType.All,
            "shared project",
            setOf(friendKey),
        ).blockingGet()

        now += 1.hours

        val createParameters = EditDelegate.CreateParameters("task")

        val taskKey = domainUpdater(now).createScheduleTopLevelTask(
            DomainListenerManager.NotificationType.All,
            createParameters,
            getSingleScheduleData(date, 5, 0),
            EditDelegate.ProjectParameters(sharedProjectKey, setOf()),
        )
            .blockingGet()
            .taskKey

        assertEquals(1, getDayInstanceDatas(now).size)

        now += 1.hours

        val newScheduleDatas = getSingleScheduleData(date, 6, 0)

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey,
            createParameters,
            newScheduleDatas,
            EditDelegate.ProjectParameters(sharedProjectKey, setOf()),
        ).blockingSubscribe()

        assertEquals(1, getDayInstanceDatas(now).size)

        now += 1.hours

        domainUpdater(now).updateScheduleTask(
            DomainListenerManager.NotificationType.All,
            taskKey,
            createParameters,
            newScheduleDatas,
            EditDelegate.ProjectParameters(sharedProjectKey, setOf(friendKey)),
        ).blockingSubscribe()

        assertTrue(getDayInstanceDatas(now).isEmpty())
    }
}