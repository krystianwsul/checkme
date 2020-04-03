package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

class SharedProjectsLoader(
        projectKeysObservable: Observable<Set<ProjectKey.Shared>>,
        sharedProjectManager: AndroidSharedProjectManager,
        private val domainDisposable: CompositeDisposable,
        sharedProjectsProvider: SharedProjectsProvider
) {

    private fun <T> Observable<T>.publishImmediate() = publish().apply { domainDisposable += connect() }!!

    private val projectDatabaseRxObservable = projectKeysObservable.processChangesSet(
            {
                DatabaseRx(
                        domainDisposable,
                        sharedProjectsProvider.getSharedProjectObservable(it)
                )
            },
            { it.disposable.dispose() }
    ).publishImmediate()

    val projectLoadersObservable = projectDatabaseRxObservable.processChanges(
            { it.first },
            { (_, mapChanges), projectKey ->
                ProjectLoader(
                        mapChanges.newMap
                                .getValue(projectKey)
                                .changes,
                        domainDisposable,
                        sharedProjectsProvider.projectProvider,
                        sharedProjectManager
                )
            }
    ).publishImmediate()
}