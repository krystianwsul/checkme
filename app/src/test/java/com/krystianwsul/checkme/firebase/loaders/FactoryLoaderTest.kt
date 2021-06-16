package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.*
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.projects.SharedProjectJson
import com.krystianwsul.common.firebase.json.tasks.PrivateTaskJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.tasks.SharedTaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TaskKeyData
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.junit.*
import org.junit.Assert.*

class FactoryLoaderTest {

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val local = object : Instance.ShownFactory {

        override fun getShown(taskKey: TaskKey, scheduleDateTime: DateTime): Instance.Shown? = null

        override fun createShown(taskKeyData: TaskKeyData, scheduleDateTime: DateTime) = object : Instance.Shown {

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

    private class TestDatabase : FactoryProvider.Database() {

        val privateProjectObservable = PublishRelay.create<PrivateProjectJson>()!!
        val sharedProjectObservable = PublishRelay.create<Snapshot<JsonWrapper>>()!!
        val userObservable = PublishRelay.create<Snapshot<UserWrapper>>()!!

        override fun getPrivateProjectObservable(key: ProjectKey.Private) =
            privateProjectObservable.map { Snapshot(key.key, it) }!!

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectObservable

        override fun getUserObservable(userKey: UserKey) = userObservable

        override fun getRootTaskObservable(rootTaskKey: TaskKey.Root): Observable<Snapshot<RootTaskJson>> {
            TODO("Not yet implemented")
        }

        override fun getNewId(path: String) = "id"

        override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
    }

    private class TestFactoryProvider : FactoryProvider {

        override val nullableInstance: FactoryProvider.Domain? = null

        override val database = TestDatabase()

        override val projectProvider = object : ProjectProvider {

            override val database = this@TestFactoryProvider.database
        }

        val domain = ExpectTestDomain()

        val shownFactory = mockk<Instance.ShownFactory>()

        override val shownFactorySingle = Single.just(shownFactory)!!

        override val domainUpdater = mockk<DomainUpdater>(relaxed = true)

        override val notificationStorageFactory = mockk<FactoryProvider.NotificationStorageFactory> {
            every { getNotificationStorage() } returns Single.just(mockk(relaxed = true))
        }

        override fun newDomain(
            localFactory: Instance.ShownFactory,
            myUserFactory: MyUserFactory,
            projectsFactory: ProjectsFactory,
            friendsFactory: FriendsFactory,
            deviceDbInfo: DeviceDbInfo,
            startTime: ExactTimeStamp.Local,
            readTime: ExactTimeStamp.Local,
            domainDisposable: CompositeDisposable,
            rootTasksFactory: RootTasksFactory,
            notificationStorage: FactoryProvider.NotificationStorage,
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

        factoryLoader = FactoryLoader(
            local,
            userInfoObservable,
            testFactoryProvider,
            tokenObservable,
            Single.just("uuid"),
        )

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
            .userObservable
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
            .accept(
                Snapshot(
                    sharedProjectKey,
                    JsonWrapper(SharedProjectJson(users = mutableMapOf(userInfo.key.key to UserJson()))),
                )
            )

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedTask() {
        val sharedProjectKey = "sharedProject"

        testFactoryProvider.database
            .userObservable
            .accept(
                Snapshot(
                    userInfo.key.key,
                    UserWrapper(projects = mutableMapOf(sharedProjectKey to true)),
                )
            )

        val privateTaskKey = "privateTask"

        testFactoryProvider.database
            .privateProjectObservable
            .accept(
                PrivateProjectJson(
                    tasks = mutableMapOf(privateTaskKey to PrivateTaskJson(name = privateTaskKey))
                )
            )

        val sharedTaskKey = "sharedTask"

        testFactoryProvider.database
            .sharedProjectObservable
            .accept(
                Snapshot(
                    sharedProjectKey,
                    JsonWrapper(
                        SharedProjectJson(
                            users = mutableMapOf(userInfo.key.key to UserJson()),
                            tasks = mutableMapOf(sharedTaskKey to SharedTaskJson(name = sharedTaskKey))
                        )
                    ),
                )
            )

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testPrivateAndSharedInstances() {
        testFactoryProvider.database
            .userObservable
            .accept(
                Snapshot(
                    userInfo.key.key,
                    UserWrapper(projects = mutableMapOf(sharedProjectKey to true)),
                )
            )

        testFactoryProvider.database
            .privateProjectObservable
            .accept(
                PrivateProjectJson(
                    tasks = mutableMapOf(privateTaskKey to PrivateTaskJson(name = privateTaskKey))
                )
            )

        testFactoryProvider.database
            .sharedProjectObservable
            .accept(
                Snapshot(
                    sharedProjectKey,
                    JsonWrapper(
                        SharedProjectJson(
                            users = mutableMapOf(userInfo.key.key to UserJson()),
                            tasks = mutableMapOf(sharedTaskKey to SharedTaskJson(name = sharedTaskKey))
                        )
                    ),
                )
            )

        assertNotNull(domainFactoryRelay.value)
    }

    @Test
    fun testAddPrivateTask() {
        initializeEmpty()

        testFactoryProvider.apply {

            domain.checkChange {
                database.privateProjectObservable.accept(
                    PrivateProjectJson(tasks = mutableMapOf(privateTaskKey to PrivateTaskJson("task")))
                )
            }
        }
    }
}