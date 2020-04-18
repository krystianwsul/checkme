package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.*
import com.krystianwsul.checkme.firebase.managers.AndroidPrivateProjectManager
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectFactoryTest {

    private class TestFactoryProvider : FactoryProvider {

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override val database = object : FactoryProvider.Database() {

            override fun getFriendObservable(userKey: UserKey): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getPrivateProjectObservable(key: ProjectKey.Private): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getUserObservable(key: UserKey): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
                TODO("Not yet implemented")
            }
        }

        override val nullableInstance: FactoryProvider.Domain?
            get() = TODO("Not yet implemented")

        override val preferences: FactoryProvider.Preferences
            get() = TODO("Not yet implemented")

        override val shownFactory = mockk<Instance.ShownFactory>()

        override fun newDomain(localFactory: FactoryProvider.Local, remoteUserFactory: RemoteUserFactory, projectsFactory: ProjectsFactory, deviceDbInfo: DeviceDbInfo, startTime: ExactTimeStamp, readTime: ExactTimeStamp, friendSnapshot: Snapshot): FactoryProvider.Domain {
            TODO("Not yet implemented")
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var factoryProvider: FactoryProvider
    private lateinit var projectObservable: PublishRelay<Snapshot>
    private lateinit var projectManager: AndroidPrivateProjectManager

    private lateinit var projectLoader: ProjectLoader<ProjectType.Private>
    private lateinit var projectFactory: PrivateProjectFactory

    private lateinit var changeTypesEmissionChecker: EmissionChecker<ChangeType>

    private val userInfo = UserInfo("email", "name")

    private val projectKey = ProjectKey.Private("projectKey")

    @Before
    fun before() {
        mockBase64()

        rxErrorChecker = RxErrorChecker()

        factoryProvider = TestFactoryProvider()
        projectObservable = PublishRelay.create()

        projectManager = AndroidPrivateProjectManager(
                userInfo,
                factoryProvider.database
        )

        projectLoader = ProjectLoader(
                projectObservable,
                compositeDisposable,
                factoryProvider.projectProvider,
                projectManager
        )

        projectLoader.initialProjectEvent
                .subscribeBy {
                    projectFactory = PrivateProjectFactory(
                            projectLoader,
                            it.data,
                            factoryProvider,
                            compositeDisposable
                    )

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
        projectObservable.accept(ValueTestSnapshot(PrivateProjectJson(), projectKey.key))
    }
}