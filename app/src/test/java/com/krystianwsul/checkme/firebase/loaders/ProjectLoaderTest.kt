package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class ProjectLoaderTest {

    private class TestProjectProvider : ProjectProvider() {

        private val rootInstanceObservables = mutableMapOf<String, PublishRelay<FactoryProvider.Database.Snapshot>>()

        fun acceptInstance(
                projectId: String,
                taskId: String,
                map: Map<String, Map<String, InstanceJson>>
        ) = rootInstanceObservables.getValue("$projectId-$taskId").accept(FactoryLoaderTest.ValueTestSnapshot(map))

        override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<FactoryProvider.Database.Snapshot> {
            if (!rootInstanceObservables.containsKey(taskFirebaseKey))
                rootInstanceObservables[taskFirebaseKey] = PublishRelay.create()
            return rootInstanceObservables.getValue(taskFirebaseKey)
        }

        override fun getNewId(path: String): String {
            TODO("Not yet implemented")
        }

        override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
            TODO("Not yet implemented")
        }
    }

    private lateinit var projectRecordRelay: BehaviorRelay<ProjectRecord<ProjectType.Private>>
    private val compositeDisposable = CompositeDisposable()
    private lateinit var projectProvider: TestProjectProvider
    private lateinit var projectLoader: ProjectLoader<ProjectType.Private>

    private inner class EmissionTester<T>(
            private val publishRelay: PublishRelay<T> = PublishRelay.create()
    ) : Consumer<T> by publishRelay {

        private val handlers = mutableListOf<(T) -> Unit>()

        init {
            compositeDisposable += publishRelay.subscribe {
                handlers.first().invoke(it)
                handlers.removeFirst()
            }
        }

        fun addHandler(handler: (T) -> Unit) {
            handlers += handler
        }

        fun checkEmpty() = assertTrue(handlers.isEmpty())
    }

    private lateinit var addProjectEmissionTester: EmissionTester<ProjectLoader.AddProjectEvent<ProjectType.Private>>
    private lateinit var addTaskEmissionTester: EmissionTester<ProjectLoader.AddTaskEvent<ProjectType.Private>>
    private lateinit var changeInstancesEmissionTester: EmissionTester<ProjectLoader.ChangeInstancesEvent<ProjectType.Private>>

    private val projectKey = ProjectKey.Private("projectKey")

    private lateinit var errors: MutableList<Throwable>

    @Before
    fun before() {
        errors = mutableListOf()
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            errors.add(it)
        }

        projectRecordRelay = BehaviorRelay.create()
        projectProvider = TestProjectProvider()

        projectLoader = ProjectLoader(
                projectRecordRelay,
                compositeDisposable,
                projectProvider
        )

        addProjectEmissionTester = EmissionTester()
        addTaskEmissionTester = EmissionTester()
        changeInstancesEmissionTester = EmissionTester()

        projectLoader.addProjectEvent
                .subscribe(addProjectEmissionTester)
                .addTo(compositeDisposable)

        projectLoader.addTaskEvents
                .subscribe(addTaskEmissionTester)
                .addTo(compositeDisposable)

        projectLoader.changeInstancesEvents
                .subscribe(changeInstancesEmissionTester)
                .addTo(compositeDisposable)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        addProjectEmissionTester.checkEmpty()
        addTaskEmissionTester.checkEmpty()
        changeInstancesEmissionTester.checkEmpty()

        assertTrue(errors.isEmpty())
    }

    @Test
    fun testInitial() {
        assertNull(projectLoader.addProjectEvent.tryGetCurrentValue())
    }

    @Test
    fun testEmptyProject() {
        addProjectEmissionTester.addHandler { }

        projectRecordRelay.accept(PrivateProjectRecord(
                projectProvider,
                projectKey,
                PrivateProjectJson()
        ))
    }
}