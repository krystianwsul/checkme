package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.ReplayRelay
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.UserKeyStore
import com.krystianwsul.checkme.firebase.managers.AndroidSharedProjectManager
import com.krystianwsul.checkme.firebase.roottask.LoadDependencyTrackerManager
import com.krystianwsul.checkme.firebase.roottask.ProjectToRootTaskCoordinator
import com.krystianwsul.checkme.firebase.roottask.RootTaskKeySource
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

interface SharedProjectsLoader {

    val projectManager: AndroidSharedProjectManager

    val initialProjectsEvent: Single<InitialProjectsEvent>

    // this is the event for adding new projects
    val addProjectEvents: Observable<ChangeWrapper<AddProjectEvent>>

    val removeProjectEvents: Observable<RemoveProjectsEvent>

    fun addProject(jsonWrapper: JsonWrapper): SharedProjectRecord

    class Impl(
            projectKeysObservable: Observable<Set<ProjectKey.Shared>>,
            override val projectManager: AndroidSharedProjectManager,
            private val domainDisposable: CompositeDisposable,
            private val sharedProjectsProvider: SharedProjectsProvider,
            private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
            private val userKeyStore: UserKeyStore,
            private val projectToRootTaskCoordinator: ProjectToRootTaskCoordinator,
            private val rootTaskKeySource: RootTaskKeySource,
            private val loadDependencyTrackerManager: LoadDependencyTrackerManager,
    ) : SharedProjectsLoader {

        private data class AddedProjectData(val initialProjectRecord: SharedProjectRecord)

        private val addedProjectDatasRelay =
                ReplayRelay.create<ChangeWrapper<Map<ProjectKey.Shared, AddedProjectData?>>>()

        init {
            projectKeysObservable.map {
                ChangeWrapper<Map<ProjectKey.Shared, AddedProjectData?>>(ChangeType.REMOTE, it.associateWith { null })
            }
                    .subscribe(addedProjectDatasRelay)
                    .addTo(domainDisposable)
        }

        private fun <T> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }!!
        private fun <T> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }!!

        private data class ProjectData(
                val userChangeType: ChangeType,
                val projectKeys: Set<ProjectKey.Shared>,
                val newMap: Map<ProjectKey.Shared, ProjectEntry>,
        )

        private data class ProjectEntry(
                val userChangeType: ChangeType,
                val databaseRx: DatabaseRx<Snapshot<JsonWrapper>>,
                val initialProjectRecord: SharedProjectRecord?,
        )

        private val projectDatabaseRxObservable = addedProjectDatasRelay.distinctUntilChanged()
                .processChanges(
                        { it.data.keys },
                        { (changeType, addedProjectDatas), projectKey ->
                            val addedProjectData = addedProjectDatas.getValue(projectKey)

                            ProjectEntry(
                                    changeType,
                                    DatabaseRx(
                                            domainDisposable,
                                            sharedProjectsProvider.getSharedProjectObservable(projectKey),
                                    ),
                                    addedProjectData?.initialProjectRecord,
                            )
                        },
                        { it.databaseRx.disposable.dispose() },
                )
                .map { ProjectData(it.original.changeType, it.original.data.keys, it.newMap) }
                .replayImmediate()

        private data class LoaderData(
                val userChangeType: ChangeType,
                val newLoaderMap: Map<ProjectKey.Shared, ProjectLoader<ProjectType.Shared, JsonWrapper>>,
                val addedLoaderEntries: Map<ProjectKey.Shared, ProjectLoader<ProjectType.Shared, JsonWrapper>>,
                val removedProjectKeys: Set<ProjectKey.Shared>,
        )

        private val projectLoadersObservable = projectDatabaseRxObservable.processChanges(
                { it.projectKeys },
                { mapChanges, projectKey ->
                    val projectEntry = mapChanges.newMap.getValue(projectKey)

                    if (projectEntry.initialProjectRecord != null) {
                        check(mapChanges.userChangeType == ChangeType.LOCAL)
                    }

                    ProjectLoader.Impl(
                            projectEntry.databaseRx.observable,
                            domainDisposable,
                            projectManager,
                            projectEntry.initialProjectRecord,
                            userCustomTimeProviderSource,
                            projectToRootTaskCoordinator,
                            loadDependencyTrackerManager,
                    )
                }
        )
                .map {
                    LoaderData(
                            it.original.userChangeType,
                            it.newMap,
                            it.addedEntries,
                            it.removedEntries.keys,
                    )
                }
                .replayImmediate()

        override val initialProjectsEvent = projectLoadersObservable.firstOrError()
                .flatMap {
                    it.newLoaderMap
                            .values
                            .map { projectLoader ->
                                projectLoader.initialProjectEvent
                                        .doOnSuccess { check(it.changeType == ChangeType.REMOTE) }
                                        .map { projectLoader to it }
                            }
                            .zipSingle()
                }
                .map {
                    check(it.all { it.second.changeType == ChangeType.REMOTE })

                    InitialProjectsEvent(it.map { InitialProjectData(it.first, it.second.data) })
                }
                .cacheImmediate()

        override val addProjectEvents = projectLoadersObservable.skip(1)
                .switchMap {
                    it.addedLoaderEntries
                            .values
                            .map { projectLoader ->
                                projectLoader.initialProjectEvent
                                        .map { it.newData(AddProjectEvent(projectLoader, it.data)) }
                                        .toObservable()
                            }
                            .merge()
                }
                .replayImmediate()

        override val removeProjectEvents = projectLoadersObservable.filter { it.removedProjectKeys.isNotEmpty() }
                .map {
                    check(it.userChangeType == ChangeType.REMOTE)

                    RemoveProjectsEvent(it.removedProjectKeys)
                }
                .doOnNext {
                    rootTaskKeySource.onProjectsRemoved(it.projectKeys)
                    userKeyStore.onProjectsRemoved(it.projectKeys)
                }
                .replayImmediate()

        override fun addProject(jsonWrapper: JsonWrapper): SharedProjectRecord {
            val sharedProjectRecord = projectManager.newProjectRecord(jsonWrapper)

            val addedProjectDatas = addedProjectDatasRelay.value!!
                    .data
                    .toMutableMap()

            check(!addedProjectDatas.containsKey(sharedProjectRecord.projectKey))

            addedProjectDatas[sharedProjectRecord.projectKey] = AddedProjectData(sharedProjectRecord)

            addedProjectDatasRelay.accept(ChangeWrapper(ChangeType.LOCAL, addedProjectDatas))

            return sharedProjectRecord
        }
    }

    class InitialProjectsEvent(val initialProjectDatas: List<InitialProjectData>)

    data class InitialProjectData(
            val projectLoader: ProjectLoader<ProjectType.Shared, JsonWrapper>,
            val initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared, JsonWrapper>,
    )

    class AddProjectEvent(
            val projectLoader: ProjectLoader<ProjectType.Shared, JsonWrapper>,
            val initialProjectEvent: ProjectLoader.InitialProjectEvent<ProjectType.Shared, JsonWrapper>,
    )

    class RemoveProjectsEvent(val projectKeys: Set<ProjectKey.Shared>)
}