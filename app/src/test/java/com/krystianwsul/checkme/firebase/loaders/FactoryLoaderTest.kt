package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.ProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockkStatic
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

        override fun updateFriendRecords(dataSnapshot: FactoryProvider.Database.Snapshot) {
            TODO("Not yet implemented")
        }

        override fun updateInstanceRecords(instanceEvent: ProjectFactory.InstanceEvent) {
            TODO("Not yet implemented")
        }

        override fun updatePrivateProjectRecord(dataSnapshot: FactoryProvider.Database.Snapshot) {
            TODO("Not yet implemented")
        }

        override fun updateSharedProjectRecords(sharedProjectEvent: ProjectFactory.SharedProjectEvent) {
            TODO("Not yet implemented")
        }

        override fun updateUserRecord(dataSnapshot: FactoryProvider.Database.Snapshot) {
            TODO("Not yet implemented")
        }
    }

    private class ExpectTestDomain : TestDomain() {

        private var userListener: ((dataSnapshot: FactoryProvider.Database.Snapshot) -> Unit)? = null
        private var privateListenerWrapper: ListenerWrapper<FactoryProvider.Database.Snapshot>? = null
        private var sharedListener: ((sharedProjectEvent: ProjectFactory.SharedProjectEvent) -> Unit)? = null
        private var instanceListener: ((instanceEvent: ProjectFactory.InstanceEvent) -> Unit)? = null

        class ListenerWrapper<T> {

            var result: T? = null
        }

        fun checkUser(listener: (dataSnapshot: FactoryProvider.Database.Snapshot) -> Unit) {
            assertNull(userListener)

            userListener = listener
        }

        fun checkPrivate(listener: ListenerWrapper<FactoryProvider.Database.Snapshot>.() -> Unit) {
            assertNull(privateListenerWrapper)

            privateListenerWrapper = ListenerWrapper()

            privateListenerWrapper!!.listener()

            assertNotNull(privateListenerWrapper!!.result)

            privateListenerWrapper = null
        }

        fun checkShared(listener: (sharedProjectEvent: ProjectFactory.SharedProjectEvent) -> Unit) {
            assertNull(sharedListener)

            sharedListener = listener
        }

        fun checkInstance(listener: (instanceEvent: ProjectFactory.InstanceEvent) -> Unit) {
            assertNull(instanceListener)

            instanceListener = listener
        }

        override fun updateUserRecord(dataSnapshot: FactoryProvider.Database.Snapshot) {
            assertNotNull(userListener)

            userListener!!(dataSnapshot)

            userListener = null
        }

        override fun updatePrivateProjectRecord(dataSnapshot: FactoryProvider.Database.Snapshot) {
            assertNotNull(privateListenerWrapper)
            assertNull(privateListenerWrapper!!.result)

            privateListenerWrapper!!.result = dataSnapshot
        }

        override fun updateSharedProjectRecords(sharedProjectEvent: ProjectFactory.SharedProjectEvent) {
            assertNotNull(sharedListener)

            sharedListener!!(sharedProjectEvent)

            sharedListener = null
        }

        override fun updateInstanceRecords(instanceEvent: ProjectFactory.InstanceEvent) {
            assertNotNull(instanceListener)

            instanceListener!!(instanceEvent)

            instanceListener = null
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
        ) = rootInstanceObservables.getValue("$projectId-$taskId").accept(ValueTestSnapshot(map))

        override fun getFriendObservable(userKey: UserKey) = friendObservable

        override fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectObservable.map<Snapshot> { ValueTestSnapshot(it) }!!

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectObservable

        override fun getUserObservable(key: UserKey) = userObservable

        override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
            if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()
            return rootInstanceObservables.getValue(taskFirebaseKey)
        }

        override fun getNewId(path: String) = "id"

        override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) = Unit
    }

    private class TestPreferences : FactoryProvider.Preferences {

        override var tab = 0
    }

    private class TestFactoryProvider : FactoryProvider {

        override val nullableInstance: FactoryProvider.Domain? = null

        override val database = TestDatabase()

        override val preferences = TestPreferences()

        val domain = ExpectTestDomain()

        override fun newDomain(
                localFactory: FactoryProvider.Local,
                remoteUserFactory: RemoteUserFactory,
                projectFactory: ProjectFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp,
                readTime: ExactTimeStamp,
                friendSnapshot: FactoryProvider.Database.Snapshot
        ) = domain
    }

    private val userInfo by lazy { UserInfo("email", "name") }
    private val deviceInfoWrapper by lazy { NullableWrapper(DeviceInfo(userInfo, "token")) }

    private open class TestSnapshot : FactoryProvider.Database.Snapshot {

        override val key: String?
            get() = TODO("Not yet implemented")

        override val children: Iterable<FactoryProvider.Database.Snapshot>
            get() = TODO("Not yet implemented")

        override fun exists(): Boolean {
            TODO("Not yet implemented")
        }

        override fun <T> getValue(valueType: Class<T>): T? {
            TODO("Not yet implemented")
        }

        override fun <T> getValue(typeIndicator: FactoryProvider.Database.TypeIndicator<T>): T? {
            TODO("Not yet implemented")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class ValueTestSnapshot(private val value: Any, override val key: String? = null) : TestSnapshot() {

        override fun exists() = true

        override fun <T> getValue(valueType: Class<T>) = value as T

        override fun <T> getValue(typeIndicator: FactoryProvider.Database.TypeIndicator<T>) = value as T
    }

    private val compositeDisposable = CompositeDisposable()

    private val privateProjectKey by lazy { userInfo.key.key }
    private val sharedProjectKey = "sharedProject"
    private val privateTaskKey = "privateTask"
    private val sharedTaskKey = "sharedTask"

    private lateinit var deviceInfoObservable: PublishRelay<NullableWrapper<DeviceInfo>>
    private lateinit var testFactoryProvider: TestFactoryProvider
    private lateinit var factoryLoader: FactoryLoader
    private lateinit var domainFactoryRelay: BehaviorRelay<NullableWrapper<FactoryProvider.Domain>>

    private lateinit var errors: MutableList<Throwable>
    
    @Before
    fun before() {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } returns "key"

        errors = mutableListOf()
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            errors.add(it)
        }

        deviceInfoObservable = PublishRelay.create()
        testFactoryProvider = TestFactoryProvider()
        factoryLoader = FactoryLoader(local, deviceInfoObservable, testFactoryProvider)
        domainFactoryRelay = BehaviorRelay.create()

        factoryLoader.domainFactoryObservable
                .subscribe(domainFactoryRelay)
                .addTo(compositeDisposable)

        deviceInfoObservable.accept(deviceInfoWrapper)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        assertTrue(errors.isEmpty())
    }

    private fun initializeEmpty() {
        testFactoryProvider.database
                .userObservable
                .accept(ValueTestSnapshot(UserWrapper()))

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
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true))))

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
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true))))

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
                .accept(ValueTestSnapshot(UserWrapper(projects = mutableMapOf(sharedProjectKey to true))))

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
            domain.checkPrivate {
                database.privateProjectObservable.accept(PrivateProjectJson(tasks = mutableMapOf(privateTaskKey to TaskJson())))
            }

            domain.checkInstance { }
            database.acceptInstance(privateProjectKey, privateTaskKey, mapOf("2019-03-25" to mapOf("16-44" to InstanceJson())))
        }
    }
}