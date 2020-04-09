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
        sharedProjectManager: AndroidSharedProjectManager,
        private val domainDisposable: CompositeDisposable,
        sharedProjectsProvider: SharedProjectsProvider
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
                        sharedProjectManager
                )
            }
    ).publishImmediate()

    // this is the initial set of projects, plus their instances
    val initialProjectsEvent = projectLoadersObservable.firstOrError()
            .flatMap {
                it.second
                        .newMap
                        .values
                        .map { it.initialProjectEvent }
                        .zipSingle()
            }
            .map { InitialProjectsEvent(it) }
            .cacheImmediate()

    val removeProjectEvents = projectLoadersObservable.map {
        RemoveProjectsEvent(it.second.removedEntries.keys)
    }
            .filter { it.projectKeys.isNotEmpty() }
            .publishImmediate()

    // this is the event for adding new projects
    val addProjectEvents = projectLoadersObservable.skip(1)
            .switchMap {
                it.second.addedEntries
                        .values
                        .map { it.initialProjectEvent.toObservable() }
                        .merge()
            }
            .publishImmediate()

    val addTaskEvents = projectLoadersObservable.switchMap {
        it.second
                .newMap
                .values
                .map { it.addTaskEvents }
                .merge()
    }.publishImmediate()

    val changeInstancesEvents = projectLoadersObservable.switchMap {
        it.second
                .newMap
                .values
                .map { it.changeInstancesEvents }
                .merge()
    }.publishImmediate()

    val changeProjectEvents = projectLoadersObservable.switchMap {
        it.second
                .newMap
                .values
                .map { it.changeProjectEvents }
                .merge()
    }.publishImmediate()

    class InitialProjectsEvent(val initialProjectEvents: List<ProjectLoader.InitialProjectEvent<ProjectType.Shared>>)

    class RemoveProjectsEvent(val projectKeys: Set<ProjectKey.Shared>)
}