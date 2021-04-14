package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.DomainFactoryRule
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.SharedProjectJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@ExperimentalStdlibApi
class ProjectFactoryTest {

    class TestFactoryProvider : FactoryProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<Snapshot<JsonWrapper>>>()

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override val database = object : FactoryProvider.Database() {

            override fun getPrivateProjectObservable(key: ProjectKey.Private): Observable<Snapshot<PrivateProjectJson>> {
                TODO("Not yet implemented")
            }

            override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot<JsonWrapper>> {
                if (!sharedProjectObservables.containsKey(projectKey))
                    sharedProjectObservables[projectKey] = PublishRelay.create()
                return sharedProjectObservables.getValue(projectKey)
            }

            override fun getUserObservable(userKey: UserKey): Observable<Snapshot<UserWrapper>> {
                TODO("Not yet implemented")
            }

            override fun getNewId(path: String) = Random.nextInt().toString()

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) = Unit
        }

        override val nullableInstance: FactoryProvider.Domain
            get() = TODO("Not yet implemented")

        override val shownFactory = mockk<Instance.ShownFactory>(relaxed = true)

        override val domainUpdater: DomainUpdater
            get() = TODO("Not yet implemented")

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
            TODO("Not yet implemented")
        }

        fun acceptSharedProject(
                projectKey: ProjectKey.Shared,
                projectJson: SharedProjectJson
        ) {
            sharedProjectObservables.getValue(projectKey).accept(Snapshot(
                    projectKey.key,
                    JsonWrapper(projectJson),
            ))
        }
    }

    class TestProjectLoader(projectKey: ProjectKey.Private) : ProjectLoader<ProjectType.Private, PrivateProjectJson> {

        private val userInfo = UserInfo("email", "name", "uid")

        override val projectManager = AndroidPrivateProjectManager(userInfo, mockk(relaxed = true))

        val projectRecord = PrivateProjectRecord(mockk(), projectKey, PrivateProjectJson())

        private val event = ProjectLoader.InitialProjectEvent(projectManager, projectRecord)

        override val initialProjectEvent = Single.just(ChangeWrapper(ChangeType.REMOTE, event))!!

        override val changeProjectEvents = PublishRelay.create<ChangeWrapper<ProjectLoader.ChangeProjectEvent<ProjectType.Private>>>()!!
    }

    @get:Rule
    val domainFactoryRule = DomainFactoryRule()

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var factoryProvider: FactoryProvider
    private lateinit var projectLoader: TestProjectLoader

    private lateinit var projectFactory: PrivateProjectFactory

    private lateinit var changeTypesEmissionChecker: EmissionChecker<ChangeType>

    private val projectKey = ProjectKey.Private("projectKey")

    @Before
    fun before() {
        mockBase64()

        rxErrorChecker = RxErrorChecker()

        factoryProvider = TestFactoryProvider()
        projectLoader = TestProjectLoader(projectKey)

        projectLoader.initialProjectEvent
                .subscribeBy {
                    projectFactory = PrivateProjectFactory(
                            projectLoader,
                            it.data,
                            factoryProvider,
                            compositeDisposable,
                            mockk(),
                    ) { mockk() }

                    changeTypesEmissionChecker = EmissionChecker("changeTypes", compositeDisposable, projectFactory.changeTypes)
                }
                .addTo(compositeDisposable)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        changeTypesEmissionChecker.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {

    }
}