package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class SharedProjectsLoaderTest {

    private class TestSharedProjectsProvider : SharedProjectsProvider {

        private val sharedProjectObservables = mutableMapOf<ProjectKey.Shared, PublishRelay<FactoryProvider.Database.Snapshot>>()

        override val projectProvider = ProjectLoaderTest.TestProjectProvider()

        override fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<FactoryProvider.Database.Snapshot> {
            if (!sharedProjectObservables.containsKey(projectKey))
                sharedProjectObservables[projectKey] = PublishRelay.create()
            return sharedProjectObservables.getValue(projectKey)
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var sharedProjectKeysRelay: BehaviorRelay<Set<ProjectKey.Shared>>
    private lateinit var sharedProjectsProvider: TestSharedProjectsProvider
    private lateinit var sharedProjectManager: AndroidSharedProjectManager
    private lateinit var sharedProjectsLoader: SharedProjectsLoader

    private lateinit var initialProjectsEmissionChecker: EmissionChecker<SharedProjectsLoader.InitialProjectsEvent>
    private lateinit var addProjectEmissionChecker: EmissionChecker<ProjectLoader.InitialProjectEvent<ProjectType.Shared>>
    private lateinit var addTaskEmissionChecker: EmissionChecker<ProjectLoader.AddTaskEvent<ProjectType.Shared>>
    private lateinit var changeInstancesEmissionChecker: EmissionChecker<ProjectLoader.ChangeInstancesEvent<ProjectType.Shared>>
    private lateinit var changeProjectEmissionChecker: EmissionChecker<ProjectLoader.ChangeProjectEvent<ProjectType.Shared>>

    @Before
    fun before() {
        rxErrorChecker = RxErrorChecker()

        sharedProjectKeysRelay = BehaviorRelay.create()
        sharedProjectsProvider = TestSharedProjectsProvider()
        sharedProjectManager = AndroidSharedProjectManager(listOf(), sharedProjectsProvider.projectProvider.database)

        sharedProjectsLoader = SharedProjectsLoader(
                sharedProjectKeysRelay,
                sharedProjectManager,
                compositeDisposable,
                sharedProjectsProvider
        )

        initialProjectsEmissionChecker = EmissionChecker("initialProjects", compositeDisposable, sharedProjectsLoader.initialProjectsEvent)
        addProjectEmissionChecker = EmissionChecker("addProject", compositeDisposable, sharedProjectsLoader.addProjectEvents)
        addTaskEmissionChecker = EmissionChecker("addTask", compositeDisposable, sharedProjectsLoader.addTaskEvents)
        changeInstancesEmissionChecker = EmissionChecker("changeInstances", compositeDisposable, sharedProjectsLoader.changeInstancesEvents)
        changeProjectEmissionChecker = EmissionChecker("changeProject", compositeDisposable, sharedProjectsLoader.changeProjectEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialProjectsEmissionChecker.checkEmpty()
        addProjectEmissionChecker.checkEmpty()
        addTaskEmissionChecker.checkEmpty()
        changeInstancesEmissionChecker.checkEmpty()
        changeProjectEmissionChecker.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {
        assertNull(sharedProjectsLoader.initialProjectsEvent.tryGetCurrentValue())
    }
}