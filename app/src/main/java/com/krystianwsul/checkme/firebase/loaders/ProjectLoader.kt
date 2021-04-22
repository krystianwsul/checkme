package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.roottask.RootTaskCoordinator
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.cacheImmediate
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Singles
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
            val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    )

    class ChangeProjectEvent<T : ProjectType>(
            val projectRecord: ProjectRecord<T>,
            val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    )

    class Impl<T : ProjectType, U : Parsable>(
            // U: Project JSON type
            snapshotObservable: Observable<Snapshot<U>>,
            private val domainDisposable: CompositeDisposable,
            override val projectManager: ProjectProvider.ProjectManager<T, U>,
            initialProjectRecord: ProjectRecord<T>?,
            private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
            private val rootTaskCoordinator: RootTaskCoordinator,
    ) : ProjectLoader<T, U> {

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!

        private data class ProjectRecordData<T : ProjectType>(
                val changeType: ChangeType,
                val projectRecord: ProjectRecord<T>,
                val userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        )

        private val projectRecordObservable: Observable<ProjectRecordData<T>> =
                snapshotObservable.mapNotNull { projectManager.set(it) }
                        .let {
                            if (initialProjectRecord != null) {
                                it.startWithItem(ChangeWrapper(ChangeType.LOCAL, initialProjectRecord))
                            } else {
                                it
                            }
                        }
                        .switchMapSingle { (projectChangeType, projectRecord) ->
                            /**
                             * I'm assuming here that 1. a new project doesn't have any custom times, and 2. all other
                             * project events are remote.
                             */

                            Singles.zip(
                                    rootTaskCoordinator.getRootTasks(projectRecord).toSingleDefault(Unit),
                                    userCustomTimeProviderSource.getUserCustomTimeProvider(projectRecord),
                            ).map { (_, userCustomTimeProvider) ->
                                ProjectRecordData(projectChangeType, projectRecord, userCustomTimeProvider)
                            }
                        }
                .replayImmediate()

        // first snapshot of everything
        override val initialProjectEvent = projectRecordObservable.firstOrError()
                .map {
                    ChangeWrapper(
                            it.changeType,
                            InitialProjectEvent(projectManager, it.projectRecord, it.userCustomTimeProvider),
                    )
                }
                .cacheImmediate(domainDisposable)

        // Here we observe remaining changes to the project or tasks, which don't affect the instance observables
        override val changeProjectEvents = projectRecordObservable.skip(1)
                .map {
                    check(it.changeType == ChangeType.REMOTE)

                    ChangeWrapper(
                            it.changeType,
                            ChangeProjectEvent(it.projectRecord, it.userCustomTimeProvider),
                    )
                }
                .replayImmediate()
    }
}