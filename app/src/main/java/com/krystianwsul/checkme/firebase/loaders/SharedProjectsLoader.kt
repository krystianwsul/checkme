package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign

class SharedProjectsLoader(
        projectKeysObservable: Observable<Set<ProjectKey.Shared>>,
        val projectManager: AndroidSharedProjectManager,
        private val domainDisposable: CompositeDisposable,
        private val sharedProjectsProvider: SharedProjectsProvider
) {

    private fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }!!
    private fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }!!

    private val projectDatabaseRxObservable = projectKeysObservable.distinctUntilChanged()
            .processChangesSet(
                    {
                        DatabaseRx(
                                domainDisposable,
                                sharedProjectsProvider.getSharedProjectObservable(it)
                        )
                    },
                    { it.disposable.dispose() }
            ).publishImmediate()

    private val projectLoadersObservable = projectDatabaseRxObservable.processChanges(
            { it.first },
            { (_, mapChanges), projectKey ->
                ProjectLoader(
                        mapChanges.newMap
                                .getValue(projectKey)
                                .observable,
                        domainDisposable,
                        sharedProjectsProvider.projectProvider,
                        projectManager
                )
            }
    ).publishImmediate()

    val initialProjectsEvent = projectLoadersObservable.firstOrError()
            .flatMap {
                it.second
                        .newMap
                        .values
                        .map { projectLoader -> projectLoader.initialProjectEvent.map { projectLoader to it } }
                        .zipSingle()
            }
            .map { InitialProjectsEvent(it) }
            .cacheImmediate()

    // this is the event for adding new projects
    val addProjectEvents: Observable<AddProjectEvent> = projectLoadersObservable.skip(1)
            .switchMap {
                it.second.addedEntries
                        .values
                        .map { projectLoader ->
                            projectLoader.initialProjectEvent
                                    .map { AddProjectEvent(projectLoader, it) }
                                    .toObservable()
                        }
                        .merge()
            }
            .publishImmediate()

    val removeProjectEvents = projectLoadersObservable.map {
        RemoveProjectsEvent(it.second.removedEntries.keys)
    }
            .filter { it.projectKeys.isNotEmpty() }
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