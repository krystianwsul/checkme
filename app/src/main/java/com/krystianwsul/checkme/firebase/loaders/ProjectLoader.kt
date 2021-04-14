package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface ProjectLoader<T : ProjectType, U : Parsable> { // U: Project JSON type

    val projectManager: ProjectProvider.ProjectManager<T, U>

    // first snapshot of everything
    val initialProjectEvent: Single<ChangeWrapper<InitialProjectEvent<T, U>>>

    // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
    val changeProjectEvents: Observable<ChangeWrapper<ChangeProjectEvent<T>>>

    class InitialProjectEvent<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            val projectManager: ProjectProvider.ProjectManager<T, U>,
            val projectRecord: ProjectRecord<T>,
    )

    class ChangeProjectEvent<T : ProjectType>(val projectRecord: ProjectRecord<T>)

    class Impl<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            snapshotObservable: Observable<Snapshot<U>>,
            private val domainDisposable: CompositeDisposable,
            override val projectManager: ProjectProvider.ProjectManager<T, U>,
            initialProjectRecord: ProjectRecord<T>?,
    ) : ProjectLoader<T, U> {

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

        private val projectRecordObservable = snapshotObservable.mapNotNull { projectManager.set(it) }
                .let {
                    if (initialProjectRecord != null) {
                        it.startWithItem(ChangeWrapper(ChangeType.LOCAL, initialProjectRecord))
                    } else {
                        it
                    }
                }
                .replayImmediate()

        // first snapshot of everything
        override val initialProjectEvent = projectRecordObservable.firstOrError()
                .map { it.newData(InitialProjectEvent(projectManager, it.data)) }
                .cacheImmediate(domainDisposable)

        // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
        override val changeProjectEvents = projectRecordObservable.skip(1)
                .map {
                    check(it.changeType == ChangeType.REMOTE)

                    it.newData(ChangeProjectEvent(it.data))
                }
                .replayImmediate()
    }
}