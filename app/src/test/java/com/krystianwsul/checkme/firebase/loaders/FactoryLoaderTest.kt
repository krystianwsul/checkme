package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.ProjectsFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FactoryLoaderTest {

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

        override fun onProjectsInstancesChange(changeType: ChangeType, now: ExactTimeStamp) {
            TODO("Not yet implemented")
        }

        override fun updateFriendRecords(dataSnapshot: Snapshot) {
            TODO("Not yet implemented")
        }

        override fun updateUserRecord(dataSnapshot: Snapshot) {
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

        override fun onProjectsInstancesChange(changeType: ChangeType, now: ExactTimeStamp) {
            assertNotNull(changeListenerWrapper)
            assertNull(changeListenerWrapper!!.result)

            changeListenerWrapper!!.result = changeType
        }

        override fun updateUserRecord(dataSnapshot: Snapshot) {
            assertNotNull(userListener)

            userListener!!(dataSnapshot)

            userListener = null
        }
    }

    private class TestDatabase : FactoryProvider.Database() {

        val friendObservable = PublishRelay.create<Snapshot>()
        val privateProjectObservable = PublishRelay.create<PrivateProjectJson>()
        val sharedProjectObservable = PublishRelay.create<Snapshot>()
        val userObservable = PublishRelay.create<Snapshot>()

        private val rootInstanceObservables = mutableMapOf<String, PublishRelay<Snapshot>>()

        fun acceptInstance(
                projectId: String,
                taskId: String,
                map: Map<String, Map<String, InstanceJson>>
        ) {
            val key = "$projectId-$taskId"
            rootInstanceObservables.getValue(key).accept(ValueTestSnapshot(map, key))
        }

        override fun getFriendObservable(userKey: UserKey) = friendObservable

        override fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectObservable.map<Snapshot> { ValueTestSnapshot(it, key.key) }!!

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectObservable

        override fun getUserObservable(key: UserKey) = userObservable

        override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
            if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()
            return rootInstanceObservables.getValue(taskFirebaseKey)
        }

        override fun getNewId(path: String) = "id"

        override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
    }

    private class TestPreferences : FactoryProvider.Preferences {

        override var tab = 0
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
                remoteUserFactory: RemoteUserFactory,
                projectsFactory: ProjectsFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp,
                readTime: ExactTimeStamp,
                friendSnapshot: Snapshot
        ) = domain
    }

    private val userInfo by lazy { UserInfo("email", "name") }
    private val userInfoWrapper by lazy { NullableWrapper(userInfo) }

    private val token = "token"
    private val tokenWrapper = NullableWrapper(token)

    private val compositeDisposable = CompositeDisposable()

    private val privateProjectKey by lazy { userInfo.key.key }
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

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .friendObservable
                .accept(TestSnapshot())

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

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(ValueTestSnapshot(
                        JsonWrapper(SharedProjectJson(users = mutableMapOf(userInfo.key.key to UserJson()))),
                        sharedProjectKey
                ))

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .friendObservable
                .accept(TestSnapshot())

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedTask() {
        val sharedProjectKey = "sharedProject"

        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true)), userInfo.key.key))

        testFactoryProvider.database
                .friendObservable
                .accept(TestSnapshot())

        val privateTaskKey = "privateTask"

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson(
                        tasks = mutableMapOf(privateTaskKey to TaskJson(name = privateTaskKey)))
                )

        val sharedTaskKey = "sharedTask"

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(ValueTestSnapshot(
                        JsonWrapper(SharedProjectJson(
                                users = mutableMapOf(userInfo.key.key to UserJson()),
                                tasks = mutableMapOf(sharedTaskKey to TaskJson(name = sharedTaskKey))
                        )),
                        sharedProjectKey
                ))

        assertNull(domainFactoryRelay.value)

        val privateProjectKey = userInfo.key.key

        testFactoryProvider.database.apply {
            acceptInstance(privateProjectKey, privateTaskKey, mapOf())
            acceptInstance(sharedProjectKey, sharedTaskKey, mapOf())
        }

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedInstances() {
        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true)), userInfo.key.key))

        testFactoryProvider.database
                .friendObservable
                .accept(TestSnapshot())

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson(
                        tasks = mutableMapOf(privateTaskKey to TaskJson(name = privateTaskKey)))
                )

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(ValueTestSnapshot(
                        JsonWrapper(SharedProjectJson(
                                users = mutableMapOf(userInfo.key.key to UserJson()),
                                tasks = mutableMapOf(sharedTaskKey to TaskJson(name = sharedTaskKey))
                        )),
                        sharedProjectKey
                ))

        assertNull(domainFactoryRelay.value)

        val map = mapOf("2020-03-25" to mapOf("14-47" to InstanceJson()))

        testFactoryProvider.database.apply {
            acceptInstance(privateProjectKey, privateTaskKey, map)
            acceptInstance(sharedProjectKey, sharedTaskKey, map)
        }

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testAddPrivateTask() {
        initializeEmpty()

        testFactoryProvider.apply {
            database.privateProjectObservable.accept(PrivateProjectJson(tasks = mutableMapOf(privateTaskKey to TaskJson("task"))))

            domain.checkChange {
                database.acceptInstance(privateProjectKey, privateTaskKey, mapOf("2019-03-25" to mapOf("16-44" to InstanceJson())))
            }
        }
    }
}