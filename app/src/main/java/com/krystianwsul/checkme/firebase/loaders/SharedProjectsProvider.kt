package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Observable

interface SharedProjectsProvider {

    val projectProvider: ProjectProvider

    fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<UntypedSnapshot>
}