package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.SharedProjectsLoader
import com.krystianwsul.checkme.firebase.loaders.mockBase64
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.*
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement


class DomainFactoryRule : TestRule {

    companion object {

        private val domainFactoryStartTime = ExactTimeStamp.Local(
                Date(2020, 12, 20),
                HourMinute(19, 0),
        )

        private val deviceDbInfo = DeviceDbInfo(
                DeviceInfo(
                        spyk(UserInfo("email", "name", "uid")) {
                            every { key } returns UserKey("key")
                        },
                        "token",
                ),
                "uuid",
        )
    }

    private val compositeDisposable = CompositeDisposable()

    lateinit var domainFactory: DomainFactory
        private set

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {

            override fun evaluate() {
                beforeClass()
                before()

                try {
                    base.evaluate()
                } finally {
                    after()
                    afterClass()
                }
            }
        }
    }

    private fun beforeClass() {
        MyApplication._sharedPreferences = mockk(relaxed = true)
        MyApplication._context = mockk(relaxed = true)

        mockkObject(Preferences)
        every { Preferences.tickLog } returns mockk(relaxed = true)
        every { Preferences.shortcuts = any() } returns Unit

        mockkObject(MyCrashlytics)
        every { MyCrashlytics.log(any()) } returns Unit

        mockkObject(NotificationWrapper)
        every { NotificationWrapper.instance } returns mockk(relaxed = true)

        mockkObject(BackendNotifier)
        every { BackendNotifier.notify(any(), any(), any()) } returns Unit

        mockkObject(ImageManager)
        every { ImageManager.prefetch(any(), any(), any()) } returns Unit

        mockkObject(DefaultCustomTimeCreator)
        every { DefaultCustomTimeCreator.createDefaultCustomTimes(any()) } returns Unit

        DomainThreadChecker.instance = mockk(relaxed = true)

        RxJavaPlugins.setSingleSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        mockBase64()

        ErrorLogger.instance = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    private fun before() {
        val myUserFactory = mockk<MyUserFactory> {
            every { isSaved } returns false
            every { save(any()) } returns Unit
            every { user } returns mockk(relaxed = true)
        }

        val databaseWrapper = mockk<DatabaseWrapper> {
            var sharedProjectId = 0
            every { newSharedProjectRecordId() } answers {
                ProjectKey.Shared("sharedProjectId" + ++sharedProjectId)
            }

            var privateTaskId = 0
            every { newPrivateTaskRecordId(any()) } answers { "privateTaskId" + ++privateTaskId }

            var sharedTaskId = 0
            every { newSharedTaskRecordId(any()) } answers { "sharedTaskId" + ++sharedTaskId }

            var privateScheduleId = 0
            every { newPrivateScheduleRecordId(any(), any()) } answers { "privateScheduleId" + ++privateScheduleId }

            var sharedScheduleId = 0
            every { newSharedScheduleRecordId(any(), any()) } answers { "sharedScheduleId" + ++sharedScheduleId }

            var noScheduleOrParentId = 0
            every { newPrivateNoScheduleOrParentRecordId(any(), any()) } answers {
                "noScheduleOrParentId" + ++noScheduleOrParentId
            }

            var taskHierarchyId = 0
            every { getPrivateTaskHierarchyRecordId(any()) } answers { "taskHierarchyId" + ++taskHierarchyId }

            var privateCustomTimeId = 0
            every { newPrivateCustomTimeRecordId(any()) } answers { "privateCustomTimeId" + ++privateCustomTimeId }

            var sharedCustomTimeId = 0
            every { newSharedCustomTimeRecordId(any()) } answers { "sharedCustomTimeId" + ++sharedCustomTimeId }

            every { update(any(), any()) } returns Unit
        }

        val sharedProjectsLoader = mockk<SharedProjectsLoader>(relaxed = true) {
            every { projectManager } returns spyk(AndroidSharedProjectManager(databaseWrapper)) {
                every { save(any()) } returns Unit
            }
        }

        val projectsFactory = ProjectsFactory(
                mockk(),
                mockk(relaxed = true),
                ProjectLoader.InitialProjectEvent(
                        mockk(relaxed = true),
                        PrivateProjectRecord(
                                databaseWrapper,
                                deviceDbInfo.userInfo,
                                PrivateProjectJson(
                                        startTime = domainFactoryStartTime.long,
                                        startTimeOffset = domainFactoryStartTime.offset,
                                ),
                        ),
                        mapOf(),
                ),
                sharedProjectsLoader,
                SharedProjectsLoader.InitialProjectsEvent(listOf()),
                domainFactoryStartTime,
                mockk(relaxed = true),
                compositeDisposable,
        ) { deviceDbInfo }

        val friendsFactory = mockk<FriendsFactory> {
            every { isSaved } returns false
            every { save(any()) } returns Unit
            every { getUserJsons(any()) } returns mapOf()
            every { updateProjects(any(), any(), any()) } returns Unit
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
                databaseWrapper,
        ) { TestDomainUpdater(it, ExactTimeStamp.Local.now) }
    }

    private fun after() {
        compositeDisposable.clear()
    }

    private fun afterClass() {
        MyApplication._sharedPreferences = null
        MyApplication._context = null

        unmockkObject(Preferences)

        unmockkObject(MyCrashlytics)

        unmockkObject(NotificationWrapper)

        unmockkObject(BackendNotifier)

        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }
}