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

class SharedProjectsLoader(
        projectKeysObservable: Observable<ChangeWrapper<Set<ProjectKey.Shared>>>,
        val projectManager: AndroidSharedProjectManager,
        private val domainDisposable: CompositeDisposable,
        private val sharedProjectsProvider: SharedProjectsProvider
) {

    private fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }!!
    private fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }!!

    private data class ProjectData(
            val userChangeType: ChangeType,
            val databaseRx: DatabaseRx
    )

    private val projectDatabaseRxObservable = projectKeysObservable.distinctUntilChanged()
            .processChanges(
                    { it.data },
                    { (changeType, _), projectKey -> // todo instances unit test if this changeType needs to be propagated
                        ProjectData(
                                changeType,
                                DatabaseRx(
                                        domainDisposable,
                                        sharedProjectsProvider.getSharedProjectObservable(projectKey)
                                )
                        )
                    },
                    { it.databaseRx.disposable.dispose() }
            ).publishImmediate()

    private data class LoaderData(
            val userChangeType: ChangeType,
            val mapChanges: MapChanges<ProjectKey.Shared, ProjectLoader<ProjectType.Shared>>
    )

    private val projectLoadersObservable = projectDatabaseRxObservable.processChanges(
            { it.first.data },
            { (_, mapChanges), projectKey ->
                ProjectLoader(
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
                        it.first
                                .first
                                .changeType,
                        it.second
                )
            }
            .publishImmediate()

    val initialProjectsEvent = projectLoadersObservable.firstOrError()
            .flatMap {
                it.mapChanges
                        .newMap
                        .values
                        .map { projectLoader -> projectLoader.initialProjectEvent.map { projectLoader to it } }
                        .zipSingle()
            }
            .map {
                check(it.all { it.second.changeType == ChangeType.LOCAL })

                InitialProjectsEvent(it.map { it.first to it.second.data })
            }
            .cacheImmediate()

    // this is the event for adding new projects
    val addProjectEvents = projectLoadersObservable.skip(1)
            .switchMap {
                it.mapChanges
                        .addedEntries
                        .values
                        .map { projectLoader ->
                            projectLoader.initialProjectEvent
                                    .map { (changeType, initialProjectEvent) -> ChangeWrapper(changeType, AddProjectEvent(projectLoader, initialProjectEvent)) }
                                    .toObservable()
                        }
                        .merge()
            }
            .publishImmediate()

    val removeProjectEvents = projectLoadersObservable.map {
        ChangeWrapper(
                it.userChangeType,
                RemoveProjectsEvent(it.mapChanges.removedEntries.keys)
        )
    }
            .filter { it.data.projectKeys.isNotEmpty() }
            .publishImmediate()

    class InitialProjectsEvent(
            val pairs: List<Pair<ProjectLoader<ProjectType.Shared>, ProjectLoader.InitialProjectEvent<ProjectType.Shared>>>
    )

    class AddProjectEvent(
            val projectLoader: ProjectLoader<ProjectType.Shared>,
            val initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared>
    )

    class RemoveProjectsEvent(val projectKeys: Set<ProjectKey.Shared>)
}