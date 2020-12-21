package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.createScheduleRootTask
import com.krystianwsul.checkme.domainmodel.extensions.getMainData
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.SharedProjectsLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class DomainFactoryTest {

    companion object {

        private val deviceDbInfo = DeviceDbInfo(
                DeviceInfo(
                        spyk(UserInfo("email", "name", "uid")) {
                            every { key } returns UserKey("key")
                        },
                        "token",
                ),
                "uuid",
        )

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            MyApplication.sharedPreferences = mockk(relaxed = true)
            MyApplication.context = mockk(relaxed = true)

            mockkObject(Preferences)
            every { Preferences.tickLog } returns mockk(relaxed = true)
            every { Preferences.shortcuts = any() } returns Unit

            mockkObject(MyCrashlytics)
            every { MyCrashlytics.log(any()) } returns Unit

            mockkObject(NotificationWrapper)
            every { NotificationWrapper.instance } returns mockk(relaxed = true)

            mockkObject(BackendNotifier)
            every { BackendNotifier.notify(any(), any(), any()) } returns Unit
        }
    }

    private val domainFactoryStartTime = ExactTimeStamp.Local(
            Date(2020, 12, 20),
            HourMinute(19, 0).toHourMilli(),
    )

    private val compositeDisposable = CompositeDisposable()
    private lateinit var domainFactory: DomainFactory

    @Before
    fun before() {
        val myUserFactory = mockk<MyUserFactory> {
            every { isSaved } returns false
            every { save(any()) } returns Unit
            every { user } returns mockk(relaxed = true)
        }

        val databaseWrapper = mockk<DatabaseWrapper> {
            var taskId = 0
            every { getPrivateTaskRecordId(any()) } returns "taskId" + ++taskId

            var scheduleId = 0
            every { getPrivateScheduleRecordId(any(), any()) } returns "scheduleId" + ++scheduleId
        }

        val projectsFactory = ProjectsFactory(
                mockk(),
                mockk(relaxed = true),
                ProjectLoader.InitialProjectEvent(
                        mockk(relaxed = true),
                        PrivateProjectRecord(
                                databaseWrapper,
                                deviceDbInfo.userInfo,
                                PrivateProjectJson(startTime = domainFactoryStartTime.long, startTimeOffset = domainFactoryStartTime.offset)
                        ),
                        mapOf()
                ),
                mockk(relaxed = true),
                SharedProjectsLoader.InitialProjectsEvent(listOf()),
                domainFactoryStartTime,
                mockk(relaxed = true),
                compositeDisposable,
        ) { deviceDbInfo }

        val friendsFactory = mockk<FriendsFactory> {
            every { isSaved } returns false
            every { save(any()) } returns Unit
        }

        domainFactory = DomainFactory(
                mockk(relaxed = true),
                myUserFactory,
                projectsFactory,
                friendsFactory,
                deviceDbInfo,
                domainFactoryStartTime,
                domainFactoryStartTime,
                compositeDisposable,
        )
    }

    @After
    fun after() {
        compositeDisposable.clear()
    }

    @Test
    fun testCreatingTask() {
        domainFactory.createScheduleRootTask(
                0,
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
}