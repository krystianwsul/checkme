package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable

interface SharedProjectsProvider {

    val projectProvider: ProjectProvider

    fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<FactoryProvider.Database.Snapshot>
}