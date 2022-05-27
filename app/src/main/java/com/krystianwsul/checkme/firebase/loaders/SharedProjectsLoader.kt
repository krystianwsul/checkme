package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.dependencies.RootTaskKeyStore
import com.krystianwsul.checkme.firebase.dependencies.UserKeyStore
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.projects.ProjectsLoader
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.project.SharedOwnedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface SharedProjectsLoader {

    val initialProjectsEvent:
            Single<ProjectsLoader.InitialProjectsEvent<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord>>

    // this is the event for adding new projects
    val addProjectEvents:
            Observable<ChangeWrapper<ProjectsLoader.AddProjectEvent<ProjectType.Shared, JsonWrapper, SharedOwnedProjectRecord>>>

    val removeProjectEvents: Observable<ProjectsLoader.RemoveProjectsEvent<ProjectType.Shared>>

    fun addProject(parsable: JsonWrapper): SharedOwnedProjectRecord

    fun save(values: MutableMap<String, Any?>)

    class Impl(
        projectKeysObservable: Observable<Set<ProjectKey.Shared>>,
        private val projectManager: AndroidSharedProjectManager,
        domainDisposable: CompositeDisposable,
        sharedProjectsProvider: SharedProjectsProvider,
        userCustomTimeProviderSource: UserCustomTimeProviderSource,
        private val userKeyStore: UserKeyStore,
        private val rootTaskKeyStore: RootTaskKeyStore,
    ) : ProjectsLoader<ProjectType.Shared, SharedOwnedProjectRecord, JsonWrapper>(
        projectKeysObservable,
        projectManager,
        domainDisposable,
        sharedProjectsProvider,
        userCustomTimeProviderSource,
    ), SharedProjectsLoader {

        override fun onProjectAddedOrUpdated(record: SharedOwnedProjectRecord) {
            rootTaskKeyStore.onProjectAddedOrUpdated(record.projectKey, record.rootTaskParentDelegate.rootTaskKeys)
        }

        override fun onProjectsRemoved(projectKeys: Set<ProjectKey<ProjectType.Shared>>) {
            rootTaskKeyStore.onProjectsRemoved(projectKeys)
            userKeyStore.onProjectsRemoved(projectKeys.map { it as ProjectKey.Shared }.toSet())
        }

        override fun addProject(parsable: JsonWrapper): SharedOwnedProjectRecord {
            val sharedProjectRecord = projectManager.newProjectRecord(parsable)

            val addedProjectDatas = addedProjectDatasRelay.value!!
                .data
                .toMutableMap()

            check(!addedProjectDatas.containsKey(sharedProjectRecord.projectKey))

            addedProjectDatas[sharedProjectRecord.projectKey] = AddedProjectData(sharedProjectRecord)

            addedProjectDatasRelay.accept(ChangeWrapper(ChangeType.LOCAL, addedProjectDatas))

            return sharedProjectRecord
        }
    }
}