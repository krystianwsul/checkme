package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class FactoryLoaderOldTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClassStatic() {
            Task.USE_ROOT_INSTANCES = false
        }
    }

    private val local = object : FactoryProvider.Local {

        override val uuid = "uuid"

        override fun getShown(
                projectId: ProjectKey<*>,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleCustomTimeId: CustomTimeId<*>?,
                scheduleHour: Int?,
                scheduleMinute: Int?
        ): Instance.Shown? = null

        override fun createShown(
                remoteTaskId: String,
                scheduleDateTime: DateTime,
                projectId: ProjectKey<*>
        ) = object : Instance.Shown {

            override var notified = false

            override var notificationShown = false
        }
    }

    private open class TestDomain : FactoryProvider.Domain {

        override fun clearUserInfo() {
            TODO("Not yet implemented")
        }

        override fun updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo, source: SaveService.Source) = Unit

        override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local) {
            TODO("Not yet implemented")
        }

        override fun updateUserRecord(snapshot: Snapshot) {
            TODO("Not yet implemented")
        }
    }

    private class ExpectTestDomain : TestDomain() {

        private var userListener: ((dataSnapshot: Snapshot) -> Unit)? = null
        private var changeListenerWrapper: ListenerWrapper<ChangeType>? = null

        class ListenerWrapper<T> {

            var result: T? = null
        }

        fun checkChange(listener: ListenerWrapper<ChangeType>.() -> Unit) {
            assertNull(changeListenerWrapper)

            changeListenerWrapper = ListenerWrapper()

            changeListenerWrapper!!.listener()

            assertNotNull(changeListenerWrapper!!.result)

            changeListenerWrapper = null
        }

        override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local) {
            assertNotNull(changeListenerWrapper)
            assertNull(changeListenerWrapper!!.result)

            changeListenerWrapper!!.result = changeType
        }

        override fun updateUserRecord(snapshot: Snapshot) {
            assertNotNull(userListener)

            userListener!!(snapshot)

            userListener = null
        }
    }

    private class TestDatabase : FactoryProvider.Database() {

        val privateProjectObservable = PublishRelay.create<PrivateProjectJson>()!!
        val sharedProjectObservable = PublishRelay.create<Snapshot>()!!
        val userObservable = PublishRelay.create<Snapshot>()!!

        override fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectObservable.map<Snapshot> { ValueTestSnapshot(it, key.key) }!!

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectObservable

        override fun getUserObservable(userKey: UserKey) = userObservable

        override fun getRootInstanceObservable(taskFirebaseKey: String) = Observable.just<Snapshot>(EmptyTestSnapshot())!!

        override fun getNewId(path: String) = "id"

        override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
    }

    private class TestPreferences : FactoryProvider.Preferences {

        override var tab = 0

        override var addDefaultReminder: Boolean
            get() = TODO("Not yet implemented")
            set(@Suppress("UNUSED_PARAMETER") value) {}
    }

    private class TestFactoryProvider : FactoryProvider {

        override val nullableInstance: FactoryProvider.Domain? = null

        override val database = TestDatabase()

        override val preferences = TestPreferences()

        override val projectProvider = object : ProjectProvider {

            override val database = this@TestFactoryProvider.database
        }

        val domain = ExpectTestDomain()

        override val shownFactory = mockk<Instance.ShownFactory>()

        override fun newDomain(
                localFactory: FactoryProvider.Local,
                myUserFactory: MyUserFactory,
                projectsFactory: ProjectsFactory,
                friendsFactory: FriendsFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp.Local,
                readTime: ExactTimeStamp.Local,
                domainDisposable: CompositeDisposable,
        ) = domain
    }

    private val userInfo by lazy { UserInfo("email", "name", "uid") }
    private val userInfoWrapper by lazy { NullableWrapper(userInfo) }

    private val token = "token"
    private val tokenWrapper = NullableWrapper(token)

    private val compositeDisposable = CompositeDisposable()

    private val sharedProjectKey = "sharedProject"
    private val privateTaskKey = "privateTask"
    private val sharedTaskKey = "sharedTask"

    private lateinit var userInfoObservable: PublishRelay<NullableWrapper<UserInfo>>
    private lateinit var tokenObservable: BehaviorRelay<NullableWrapper<String>>
    private lateinit var testFactoryProvider: TestFactoryProvider
    private lateinit var factoryLoader: FactoryLoader
    private lateinit var domainFactoryRelay: BehaviorRelay<NullableWrapper<FactoryProvider.Domain>>

    private lateinit var errors: MutableList<Throwable>

    @Before
    fun before() {
        mockBase64()

        errors = mutableListOf()
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            errors.add(it)
        }

        userInfoObservable = PublishRelay.create()
        tokenObservable = BehaviorRelay.createDefault(tokenWrapper)
        testFactoryProvider = TestFactoryProvider()
        factoryLoader = FactoryLoader(local, userInfoObservable, testFactoryProvider, tokenObservable)
        domainFactoryRelay = BehaviorRelay.create()

        factoryLoader.domainFactoryObservable
                .subscribe(domainFactoryRelay)
                .addTo(compositeDisposable)

        userInfoObservable.accept(userInfoWrapper)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        assertTrue(errors.isEmpty())
    }

    private fun initializeEmpty() {
        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper(), userInfo.key.key))

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testEmpty() {
        initializeEmpty()
    }

    @Test
    fun testSharedProject() {
        val sharedProjectKey = "sharedProject"

        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true)), userInfo.key.key))

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(ValueTestSnapshot(
                        JsonWrapper(SharedProjectJson(users = mutableMapOf(userInfo.key.key to UserJson()))),
                        sharedProjectKey
                ))

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedTask() {
        val sharedProjectKey = "sharedProject"

        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true)), userInfo.key.key))

        val privateTaskKey = "privateTask"

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson(
                        tasks = mutableMapOf(privateTaskKey to PrivateTaskJson(name = privateTaskKey)))
                )

        val sharedTaskKey = "sharedTask"

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(ValueTestSnapshot(
                        JsonWrapper(SharedProjectJson(
                                users = mutableMapOf(userInfo.key.key to UserJson()),
                                tasks = mutableMapOf(sharedTaskKey to SharedTaskJson(name = sharedTaskKey))
                        )),
                        sharedProjectKey
                ))

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedInstances() {
        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true)), userInfo.key.key))

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson(
                        tasks = mutableMapOf(privateTaskKey to PrivateTaskJson(name = privateTaskKey)))
                )

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(ValueTestSnapshot(
                        JsonWrapper(SharedProjectJson(
                                users = mutableMapOf(userInfo.key.key to UserJson()),
                                tasks = mutableMapOf(sharedTaskKey to SharedTaskJson(name = sharedTaskKey))
                        )),
                        sharedProjectKey
                ))

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testAddPrivateTask() {
        initializeEmpty()

        testFactoryProvider.apply {

            domain.checkChange {
                database.privateProjectObservable.accept(PrivateProjectJson(tasks = mutableMapOf(privateTaskKey to PrivateTaskJson("task"))))
            }
        }
    }
}