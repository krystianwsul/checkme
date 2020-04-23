package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign

interface SharedProjectsLoader {

    val projectManager: AndroidSharedProjectManager

    val initialProjectsEvent: Single<InitialProjectsEvent>

    // this is the event for adding new projects
    val addProjectEvents: Observable<ChangeWrapper<AddProjectEvent>>

    val removeProjectEvents: Observable<ChangeWrapper<RemoveProjectsEvent>>

    class Impl(
            projectKeysObservable: Observable<ChangeWrapper<Set<ProjectKey.Shared>>>,
            override val projectManager: AndroidSharedProjectManager,
            private val domainDisposable: CompositeDisposable,
            private val sharedProjectsProvider: SharedProjectsProvider
    ) : SharedProjectsLoader {

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!
        private fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }!!

        private data class ProjectData(
                val userChangeType: ChangeType,
                val projectKeys: Set<ProjectKey.Shared>,
                val newMap: Map<ProjectKey.Shared, ProjectEntry>
        )

        private data class ProjectEntry(
                val userChangeType: ChangeType,
                val databaseRx: DatabaseRx
        )

        private val projectDatabaseRxObservable = projectKeysObservable.distinctUntilChanged()
                .processChanges(
                        { it.data },
                        { (changeType, _), projectKey ->
                            ProjectEntry(
                                    changeType,
                                    DatabaseRx(
                                            domainDisposable,
                                            sharedProjectsProvider.getSharedProjectObservable(projectKey)
                                    )
                            )
                        },
                        { it.databaseRx.disposable.dispose() }
                )
                .map { ProjectData(it.original.changeType, it.original.data, it.newMap) }
                .replayImmediate()

        private data class LoaderData(
                val userChangeType: ChangeType,
                val newLoaderMap: Map<ProjectKey.Shared, ProjectLoader<ProjectType.Shared>>,
                val addedLoaderEntries: Map<ProjectKey.Shared, ProjectLoader<ProjectType.Shared>>,
                val removedProjectKeys: Set<ProjectKey.Shared>
        )

        private val projectLoadersObservable = projectDatabaseRxObservable.processChanges(
                { it.projectKeys },
                { mapChanges, projectKey ->
                    ProjectLoader.Impl(
                            mapChanges.newMap
                                    .getValue(projectKey)
                                    .databaseRx
                                    .observable,
                            domainDisposable,
                            sharedProjectsProvider.projectProvider,
                            projectManager
                    )
                }
        )
                .map {
                    LoaderData(
                            it.original.userChangeType,
                            it.newMap,
                            it.addedEntries,
                            it.removedEntries.keys
                    )
                }
                .replayImmediate()

        override val initialProjectsEvent = projectLoadersObservable.firstOrError()
                .flatMap {
                    it.newLoaderMap
                            .values
                            .map { projectLoader -> projectLoader.initialProjectEvent.map { projectLoader to it } }
                            .zipSingle()
                }
                .map {
                    check(it.all { it.second.changeType == ChangeType.REMOTE })

                    InitialProjectsEvent(it.map { it.first to it.second.data })
                }
                .cacheImmediate()

        override val addProjectEvents = projectLoadersObservable.skip(1)
                .switchMap {
                    it.addedLoaderEntries
                            .values
                            .map { projectLoader ->
                                projectLoader.initialProjectEvent
                                        .map { (changeType, initialProjectEvent) -> ChangeWrapper(changeType, AddProjectEvent(projectLoader, initialProjectEvent)) }
                                        .toObservable()
                            }
                            .merge()
                }
                .replayImmediate()

        override val removeProjectEvents = projectLoadersObservable.map {
            ChangeWrapper(
                    it.userChangeType,
                    RemoveProjectsEvent(it.removedProjectKeys)
            )
        }
                .filter { it.data.projectKeys.isNotEmpty() }
                .replayImmediate()
    }

    class InitialProjectsEvent(
            val pairs: List<Pair<ProjectLoader<ProjectType.Shared>, ProjectLoader.InitialProjectEvent<ProjectType.Shared>>>
    )

    class AddProjectEvent(
            val projectLoader: ProjectLoader<ProjectType.Shared>,
            val initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared>
    )

    class RemoveProjectsEvent(val projectKeys: Set<ProjectKey.Shared>)
}