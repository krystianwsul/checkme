package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.loaders.ProjectsProvider
import com.krystianwsul.checkme.firebase.projects.ProjectsLoader
import com.krystianwsul.checkme.firebase.projects.ProjectsManager
import com.krystianwsul.common.firebase.json.projects.ForeignProjectJson
import com.krystianwsul.common.firebase.records.project.ForeignProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ForeignProjectsLoader(
    privateProjectKey: ProjectKey.Private,
    projectKeysObservable: Observable<Set<ProjectKey<*>>>,
    projectsManager: ProjectsManager<ProjectType, ForeignProjectJson, ForeignProjectRecord<*>>,
    domainDisposable: CompositeDisposable,
    projectsProvider: ProjectsProvider<ProjectType, ForeignProjectJson>,
    userCustomTimeProviderSource: UserCustomTimeProviderSource,
) : ProjectsLoader<ProjectType, ForeignProjectRecord<*>, ForeignProjectJson>(
    projectKeysObservable.doOnNext { check(privateProjectKey !in it) }, // debug log key
    projectsManager,
    domainDisposable,
    projectsProvider,
    userCustomTimeProviderSource,
) {

    override fun onProjectAddedOrUpdated(record: ForeignProjectRecord<*>) {}

    override fun onProjectsRemoved(projectKeys: Set<ProjectKey<out ProjectType>>) {}
}