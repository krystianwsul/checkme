package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.rxjava3.core.Observable

interface SharedProjectsProvider {

    val projectProvider: ProjectProvider

    fun getSharedProjectObservable(projectKey: ProjectKey.Shared): Observable<TypedSnapshot<JsonWrapper>>
}