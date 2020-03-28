package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.Test

class ProjectLoaderTest {

    private class TestProjectProvider : ProjectProvider {

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
    }

    @Test
    fun testInitial() {
        val projectRecordRelay = BehaviorRelay.create<ProjectRecord<ProjectType.Private>>()
        val domainDisposable = CompositeDisposable()
        val projectProvider = TestProjectProvider()

        val projectLoader = ProjectLoader(
                projectRecordRelay,
                domainDisposable,
                projectProvider
        )
    }
}