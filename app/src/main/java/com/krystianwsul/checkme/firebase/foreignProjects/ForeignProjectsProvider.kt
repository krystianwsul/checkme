package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectsProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.json.projects.ForeignProjectJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable

class ForeignProjectsProvider(val factoryProvider: FactoryProvider) : ProjectsProvider<ProjectType, ForeignProjectJson> {

    override fun getProjectObservable(projectKey: ProjectKey<*>): Observable<out Snapshot<out ForeignProjectJson>> {
        return when (projectKey) {
            is ProjectKey.Private -> factoryProvider.database.getPrivateForeignProjectObservable(projectKey)
            is ProjectKey.Shared -> factoryProvider.database.getSharedForeignProjectObservable(projectKey)
        }
    }
}