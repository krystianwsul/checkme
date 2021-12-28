package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.firebase.TestUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.SharedProjectsLoader
import com.krystianwsul.checkme.firebase.loaders.mockBase64
import com.krystianwsul.checkme.firebase.managers.AndroidRootTasksManager
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.RootTaskDependencyCoordinator
import com.krystianwsul.checkme.firebase.roottask.RootTaskKeySource
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksLoader
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.*
import com.mindorks.scheduler.RxPS
import io.mockk.*
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Observable
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

        private val userKey = UserKey("key")

        val deviceDbInfo = DeviceDbInfo(
            DeviceInfo(
                spyk(UserInfo("email", "name", "uid")) {
                    every { key } returns userKey
                },
                "token",
            ),
            "uuid",
        )
    }

    private val compositeDisposable = CompositeDisposable()

    lateinit var domainFactory: DomainFactory
        private set

    lateinit var rootModelChangeManager: RootModelChangeManager
        private set

    private val rootTaskRelays = mutableMapOf<TaskKey.Root, PublishRelay<RootTaskJson>>()

    fun acceptRootTaskJson(taskKey: TaskKey.Root, rootTaskJson: RootTaskJson) {
        rootTaskRelays.getValue(taskKey).accept(rootTaskJson)
    }

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

        mockkObject(ShortcutQueue)
        every { ShortcutQueue.updateShortcuts(any()) } returns Unit

        DomainThreadChecker.instance = mockk(relaxed = true)

        RxJavaPlugins.setSingleSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setErrorHandler { it.printStackTrace() }

        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        RxPS.setScheduler(Schedulers.trampoline())

        mockBase64()

        ErrorLogger.instance = mockk(relaxed = true)

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
    }

    private fun before() {
        val databaseWrapper = mockk<DatabaseWrapper> {
            var sharedProjectId = 0
            every { newSharedProjectRecordId() } answers {
                ProjectKey.Shared("sharedProjectId" + ++sharedProjectId)
            }

            var userCustomTimeId = 0
            every { newRootUserCustomTimeId(any()) } answers { "userCustomTimeId" + ++userCustomTimeId }

            every { update(any(), any()) } returns Unit

            var rootTaskRecordId = 0
            every { newRootTaskRecordId() } answers { "rootTaskRecordId" + ++rootTaskRecordId }

            var rootTaskScheduleRecordId = 0
            every { newRootTaskScheduleRecordId(any()) } answers {
                ScheduleId("rootTaskScheduleRecordId" + ++rootTaskScheduleRecordId)
            }

            var rootTaskNestedTaskHierarchyRecordId = 0
            every { newRootTaskNestedTaskHierarchyRecordId(any()) } answers {
                TaskHierarchyId("rootTaskNestedTaskHierarchyRecordId" + ++rootTaskNestedTaskHierarchyRecordId)
            }
        }

        rootModelChangeManager = RootModelChangeManager()

        val myUserFactory = MyUserFactory(
            Snapshot(userKey.key, UserWrapper()),
            deviceDbInfo,
            databaseWrapper,
            rootModelChangeManager,
        )

        lateinit var projectsFactory: ProjectsFactory

        val rootTaskKeySource = mockk<RootTaskKeySource>(relaxed = true) {
            every { rootTaskKeysObservable } returns Observable.just(emptySet())
        }

        val rootTasksManager = AndroidRootTasksManager(databaseWrapper)

        val rootTasksLoaderProvider = mockk<RootTasksLoader.Provider> {
            val slot = slot<TaskKey.Root>()

            every { getRootTaskObservable(capture(slot)) } answers {
                val taskKey = slot.captured

                rootTaskRelays.getOrPut(taskKey) { PublishRelay.create() }.map { Snapshot(taskKey.taskId, it) }
            }
        }

        val rootTasksLoader = RootTasksLoader(
            rootTaskKeySource,
            rootTasksLoaderProvider,
            compositeDisposable,
            rootTasksManager,
        )

        val rootTaskDependencyCoordinator = mockk<RootTaskDependencyCoordinator> {
            every { getDependencies(any()) } returns myUserFactory.user
        }

        val rootTaskFactory = RootTasksFactory(
            rootTasksLoader,
            mockk(relaxed = true),
            rootTaskDependencyCoordinator,
            compositeDisposable,
            rootTaskKeySource,
            rootModelChangeManager,
        ) { projectsFactory }

        val sharedProjectsLoader = SharedProjectsLoader.Impl(
            Observable.just(setOf()),
            spyk(AndroidSharedProjectManager(databaseWrapper)) {
                every { save(any()) } returns Unit
            },
            compositeDisposable,
            mockk(relaxed = true) {
                every { getSharedProjectObservable(any()) } returns Observable.never()
            },
            TestUserCustomTimeProviderSource(),
            mockk(relaxed = true),
            rootTaskKeySource,
        )

        projectsFactory = ProjectsFactory(
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
                myUserFactory.user,
            ),
            sharedProjectsLoader,
            SharedProjectsLoader.InitialProjectsEvent(listOf()),
            domainFactoryStartTime,
            mockk(relaxed = true),
            compositeDisposable,
            rootTaskFactory,
            rootModelChangeManager,
        ) { deviceDbInfo }

        val friendsFactory = mockk<FriendsFactory> {
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
            rootTaskFactory,
            mockk(relaxed = true),
            mockk(relaxed = true),
        ) { TestDomainUpdater(it, ExactTimeStamp.Local.now) }
    }

    private fun after() {
        rootTaskRelays.clear()

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