package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
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
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.*
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.junit.*
import org.junit.Assert.*

class FactoryLoaderNewTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun beforeClassStatic() {
            Task.USE_ROOT_INSTANCES = true
        }
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val local = object : FactoryProvider.Local {

        override val uuid = "uuid"

        override fun getShown(
                projectId: ProjectKey<*>,
                taskId: String,
                scheduleYear: Int,
                scheduleMonth: Int,
                scheduleDay: Int,
                scheduleJsonTime: JsonTime,
        ): Instance.Shown? = null

        override fun <T : ProjectType> createShown(
                remoteTaskId: String,
                scheduleDateTime: DateTime,
                projectId: ProjectKey<*>,
        ) = object : Instance.Shown {

            override var notified = false

            override var notificationShown = false
        }
    }

    private open class TestDomain : FactoryProvider.Domain {

        override fun clearUserInfo(): Completable {
            TODO("Not yet implemented")
        }

        override fun onChangeTypeEvent(changeType: ChangeType, now: ExactTimeStamp.Local) {
            TODO("Not yet implemented")
        }
    }

    private class ExpectTestDomain : TestDomain() {

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
    }

    private class TestDatabase(private val myUserKey: UserKey) : FactoryProvider.Database() {

        val privateProjectObservable = PublishRelay.create<PrivateProjectJson>()!!
        val sharedProjectObservable = PublishRelay.create<Snapshot<JsonWrapper>>()!!
        val myUserObservable = PublishRelay.create<Snapshot<UserWrapper>>()!!

        private val rootInstanceObservables = mutableMapOf<String, PublishRelay<Snapshot<Map<String, Map<String, InstanceJson>>>>>()
        private val userObservables = mutableMapOf<UserKey, PublishRelay<Snapshot<UserWrapper>>>()

        fun acceptInstance(
                projectId: String,
                taskId: String,
                map: Map<String, Map<String, InstanceJson>>,
        ) {
            val key = "$projectId-$taskId"
            rootInstanceObservables.getValue(key).accept(Snapshot(key, map))
        }

        fun acceptUser(
                userKey: UserKey,
                userWrapper: UserWrapper,
        ) = userObservables.getValue(userKey).accept(Snapshot(userKey.key, userWrapper))

        private fun getOrInitUserObservable(userKey: UserKey): PublishRelay<Snapshot<UserWrapper>> {
            if (!userObservables.containsKey(userKey))
                userObservables[userKey] = PublishRelay.create()
            return userObservables.getValue(userKey)
        }

        override fun getPrivateProjectObservable(key: ProjectKey.Private) =
                privateProjectObservable.map<Snapshot<PrivateProjectJson>> { Snapshot(key.key, it) }!!

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectObservable

        override fun getUserObservable(userKey: UserKey): Observable<Snapshot<UserWrapper>> {
            return if (userKey == myUserKey)
                myUserObservable
            else
                getOrInitUserObservable(userKey)
        }

        override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<ProjectProvider.RootInstanceData> {
            if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()

            return rootInstanceObservables.getValue(taskFirebaseKey).map {
                ProjectProvider.RootInstanceData(true, it)
            }
        }


        override fun getNewId(path: String) = "id"

        override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
    }

    private class TestFactoryProvider(myUserKey: UserKey) : FactoryProvider {

        override val nullableInstance: FactoryProvider.Domain? = null

        override val database = TestDatabase(myUserKey)

        override val projectProvider = object : ProjectProvider {

            override val database = this@TestFactoryProvider.database
        }

        val domain = ExpectTestDomain()

        override val shownFactory = mockk<Instance.ShownFactory>()

        lateinit var friendsFactory: FriendsFactory

        override val domainUpdater get() = mockk<DomainUpdater>(relaxed = true)

        override fun newDomain(
                localFactory: FactoryProvider.Local,
                myUserFactory: MyUserFactory,
                projectsFactory: ProjectsFactory,
                friendsFactory: FriendsFactory,
                deviceDbInfo: DeviceDbInfo,
                startTime: ExactTimeStamp.Local,
                readTime: ExactTimeStamp.Local,
                domainDisposable: CompositeDisposable,
        ): FactoryProvider.Domain {
            this.friendsFactory = friendsFactory

            return domain
        }
    }

    private val userInfo by lazy { UserInfo("email", "name", "uid") }
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
        testFactoryProvider = TestFactoryProvider(userInfo.key)
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
                .myUserObservable
                .accept(Snapshot(userInfo.key.key, UserWrapper()))

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
                .myUserObservable
                .accept(
                        Snapshot(
                                userInfo.key.key,
                                UserWrapper(projects = mutableMapOf(sharedProjectKey to true)),
                        )
                )

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(Snapshot(
                        sharedProjectKey,
                        JsonWrapper(SharedProjectJson(users = mutableMapOf(userInfo.key.key to UserJson()))),
                ))

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedTask() {
        val sharedProjectKey = "sharedProject"

        testFactoryProvider.database
                .myUserObservable
                .accept(
                        Snapshot(
                                userInfo.key.key,
                                UserWrapper(projects = mutableMapOf(sharedProjectKey to true)),
                        )
                )

        val privateTaskKey = "privateTask"

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson(
                        tasks = mutableMapOf(privateTaskKey to PrivateTaskJson(name = privateTaskKey)))
                )

        val sharedTaskKey = "sharedTask"

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(Snapshot(
                        sharedProjectKey,
                        JsonWrapper(SharedProjectJson(
                                users = mutableMapOf(userInfo.key.key to UserJson()),
                                tasks = mutableMapOf(sharedTaskKey to SharedTaskJson(name = sharedTaskKey))
                        )),
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
                .myUserObservable
                .accept(
                        Snapshot(
                                userInfo.key.key,
                                UserWrapper(projects = mutableMapOf(sharedProjectKey to true)),
                        )
                )

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson(
                        tasks = mutableMapOf(privateTaskKey to PrivateTaskJson(name = privateTaskKey)))
                )

        testFactoryProvider.database
                .sharedProjectObservable
                .accept(Snapshot(
                        sharedProjectKey,
                        JsonWrapper(SharedProjectJson(
                                users = mutableMapOf(userInfo.key.key to UserJson()),
                                tasks = mutableMapOf(sharedTaskKey to SharedTaskJson(name = sharedTaskKey))
                        )),
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
            database.privateProjectObservable.accept(PrivateProjectJson(tasks = mutableMapOf(privateTaskKey to PrivateTaskJson("task"))))

            domain.checkChange {
                database.acceptInstance(privateProjectKey, privateTaskKey, mapOf("2019-03-25" to mapOf("16-44" to InstanceJson())))
            }
        }
    }

    private val friendKey1 = UserKey("friendKey1")
    private val friendKey2 = UserKey("friendKey2")

    @Test
    fun testFriendsInitial() {
        testFactoryProvider.database
                .myUserObservable
                .accept(Snapshot(
                        userInfo.key.key,
                        UserWrapper(friends = mutableMapOf(friendKey1.key to true, friendKey2.key to true)),
                ))

        testFactoryProvider.database.acceptUser(friendKey1, UserWrapper())
        testFactoryProvider.database.acceptUser(friendKey2, UserWrapper())

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNotNull(domainFactoryRelay.value)
        assertEquals(
                setOf(friendKey1, friendKey2),
                testFactoryProvider.friendsFactory
                        .friends
                        .map { it.userKey }
                        .toSet()
        )
    }

    @Test
    fun testFriendsChangeAfterDomainInit() {
        testFactoryProvider.database
                .myUserObservable
                .accept(Snapshot(
                        userInfo.key.key,
                        UserWrapper(friends = mutableMapOf(friendKey1.key to true, friendKey2.key to true)),
                ))

        testFactoryProvider.database.acceptUser(friendKey1, UserWrapper())
        testFactoryProvider.database.acceptUser(friendKey2, UserWrapper())

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNotNull(domainFactoryRelay.value)

        val changedEmail = "changed email"
        testFactoryProvider.domain.checkChange {
            testFactoryProvider.database.acceptUser(friendKey2, UserWrapper(UserJson(changedEmail)))
        }

        assertEquals(
                changedEmail,
                testFactoryProvider.friendsFactory
                        .friends
                        .single { it.userKey == friendKey2 }
                        .email
        )
    }

    @Test
    fun testFriendsChangeAfterFriendsFactoryInit() {
        testFactoryProvider.database
                .myUserObservable
                .accept(Snapshot(
                        userInfo.key.key,
                        UserWrapper(friends = mutableMapOf(friendKey1.key to true, friendKey2.key to true)),
                ))

        testFactoryProvider.database.acceptUser(friendKey1, UserWrapper())
        testFactoryProvider.database.acceptUser(friendKey2, UserWrapper())

        val changedEmail = "changed email"
        testFactoryProvider.database.acceptUser(friendKey2, UserWrapper(UserJson(changedEmail)))

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNotNull(domainFactoryRelay.value)

        assertEquals(
                changedEmail,
                testFactoryProvider.friendsFactory
                        .friends
                        .single { it.userKey == friendKey2 }
                        .email,
        )
    }

    @Test
    fun testFriendsChangeBeforeFriendsFactoryInit() {
        testFactoryProvider.database
                .myUserObservable
                .accept(Snapshot(
                        userInfo.key.key,
                        UserWrapper(friends = mutableMapOf(friendKey1.key to true, friendKey2.key to true)),
                ))

        testFactoryProvider.database.acceptUser(friendKey2, UserWrapper())

        val changedEmail = "changed email"
        testFactoryProvider.database.acceptUser(friendKey2, UserWrapper(UserJson(changedEmail)))

        testFactoryProvider.database.acceptUser(friendKey1, UserWrapper())

        assertNull(domainFactoryRelay.value)

        testFactoryProvider.database
                .privateProjectObservable
                .accept(PrivateProjectJson())

        assertNotNull(domainFactoryRelay.value)

        assertEquals(
                changedEmail,
                testFactoryProvider.friendsFactory
                        .friends
                        .single { it.userKey == friendKey2 }
                        .email
        )
    }
}